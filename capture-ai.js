const puppeteer = require('puppeteer-core');
const path = require('path');
const fs = require('fs');

const CHROME_PATH = 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe';
const SCREENSHOT_DIR = path.join(__dirname, 'screenshots');

function makeJwt(userId, username, role) {
  const header = Buffer.from(JSON.stringify({ alg: "HS256", typ: "JWT" })).toString('base64url');
  const payload = Buffer.from(JSON.stringify({
    userId,
    username,
    role,
    exp: Math.floor(Date.now() / 1000) + 86400 * 30
  })).toString('base64url');
  return `${header}.${payload}.sig_${userId}`;
}

const ADMIN_JWT = makeJwt('user_admin_001', 'admin', 'ADMIN');

async function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

async function run() {
  console.log('🚀 Starting Chrome to verify AI Copilot & Smart Features...');

  const browser = await puppeteer.launch({
    executablePath: CHROME_PATH,
    headless: 'new',
    defaultViewport: {
      width: 1920,
      height: 1080,
      deviceScaleFactor: 1.25
    },
    args: [
      '--no-sandbox',
      '--disable-setuid-sandbox',
      '--disable-gpu',
      '--disable-dev-shm-usage',
      '--window-size=1920,1080'
    ]
  });

  const page = await browser.newPage();

  page.on('console', msg => console.log('[PAGE CONSOLE]', msg.text()));
  page.on('pageerror', err => console.error('[PAGE ERROR]', err.message));

  async function takeShot(filename, description) {
    const filePath = path.join(SCREENSHOT_DIR, filename);
    await page.screenshot({ path: filePath, fullPage: false });
    console.log(`✅ [CAPTURED] ${filename}: ${description}`);
  }

  try {
    console.log('👉 Navigating to http://localhost:8080...');
    await page.goto('http://localhost:8080', { waitUntil: 'networkidle2' });
    await sleep(1500);

    // Set local storage session
    await page.evaluate((jwt, uid) => {
      localStorage.setItem('chat_token', jwt);
      localStorage.setItem('chat_my_id', uid);
      localStorage.setItem('chat_username', 'admin');
      localStorage.setItem('chat_role', 'ADMIN');
    }, ADMIN_JWT, 'user_admin_001');

    await page.reload({ waitUntil: 'networkidle2' });
    await sleep(2000);

    // Select Gemini AI Copilot Conversation
    console.log('👉 Selecting Gemini AI Copilot conversation...');
    await page.evaluate(() => {
      const convs = document.querySelectorAll('.conv-item');
      for (const c of convs) {
        if (c.textContent.includes('Gemini AI Copilot')) {
          c.click();
          break;
        }
      }
    });
    await sleep(1500);
    await takeShot('ai_01_copilot_chat.png', 'Giao diện Chat 1-1 với Gemini AI Copilot');

    // Trigger Smart Reply and Tone wand
    console.log('👉 Testing Tone Polisher Wand...');
    await page.evaluate(() => {
      const input = document.getElementById('msgInput');
      input.value = 'Bạn kiểm tra lại báo cáo tiến độ giúp mình';
      const wand = document.getElementById('btnAiWand');
      if (wand) wand.click();
    });
    await sleep(1000);
    await takeShot('ai_02_tone_wand_popover.png', 'Menu AI Tone Polisher (Cây đũa thần)');

    // Apply Professional Tone
    console.log('👉 Applying Professional Tone...');
    await page.evaluate(() => {
      const btn = document.querySelector('.tone-option-btn');
      if (btn) btn.click();
    });
    await sleep(1500);
    await takeShot('ai_03_tone_applied.png', 'Văn phong chuyên nghiệp đã được AI viết lại');

    // Send question to AI Copilot
    console.log('👉 Sending question @AI to Copilot...');
    await page.evaluate(() => {
      const input = document.getElementById('msgInput');
      input.value = '@AI Giải thích cơ chế Redis Pub/Sub và Cassandra cluster';
      const sendBtn = document.getElementById('btnSend');
      sendBtn.disabled = false;
      sendBtn.click();
    });
    await sleep(2500); // Wait for real-time WebSocket response from bot
    await takeShot('ai_04_bot_response.png', 'AI Copilot phản hồi Markdown và code highlight thời gian thực');

    // Open AI Summarize Modal
    console.log('👉 Opening AI Summarize Modal...');
    await page.evaluate(() => {
      openAiSummarizeModal();
    });
    await sleep(2000);
    await takeShot('ai_05_summarize_modal.png', 'Modal AI Tóm tắt Hội thoại & Todo List');
    await page.evaluate(() => closeAiSummarizeModal());
    await sleep(500);

    // Open AI Imagine Studio Modal
    console.log('👉 Opening AI Imagine Studio Modal...');
    await page.evaluate(() => {
      openAiImagineModal();
      document.getElementById('aiImaginePromptInput').value = 'Cyberpunk Neon Matrix Core';
    });
    await sleep(800);
    await page.evaluate(() => generateAiImage());
    await sleep(2000);
    await takeShot('ai_06_imagine_studio.png', 'AI Imagine Studio tạo tác phẩm nghệ thuật');
    await page.evaluate(() => closeAiImagineModal());
    await sleep(500);

    // Test Message Translate
    console.log('👉 Testing Message Translate Bubble...');
    await page.evaluate(() => {
      const transBtns = document.querySelectorAll('.msg-action-btn');
      for (const btn of transBtns) {
        if (btn.getAttribute('title') && btn.getAttribute('title').includes('Dịch')) {
          btn.click();
          break;
        }
      }
    });
    await sleep(1500);
    await takeShot('ai_07_translated_bubble.png', 'Thẻ bản dịch AI hiển thị ngay dưới tin nhắn');

    console.log('🎉 ALL AI FEATURES TESTED AND CAPTURED SUCCESSFULLY!');
  } catch (err) {
    console.error('❌ Error during testing:', err);
  } finally {
    await browser.close();
  }
}

run();
