// Automated Screenshot Capture Script using Puppeteer-core and Chrome
const puppeteer = require('puppeteer-core');
const path = require('path');
const fs = require('fs');

const CHROME_PATH = 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe';
const SCREENSHOT_DIR = path.join(__dirname, 'screenshots');

if (!fs.existsSync(SCREENSHOT_DIR)) {
  fs.mkdirSync(SCREENSHOT_DIR, { recursive: true });
}

async function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

async function run() {
  console.log('🚀 Starting Chrome for Capturing Admin Screens...');
  
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

  // Dismiss any unexpected dialogs automatically
  page.on('dialog', async dialog => {
    console.log(`[DIALOG] ${dialog.type()}: ${dialog.message()}`);
    await dialog.dismiss();
  });

  // Pre-seed localStorage before any page navigation
  await page.evaluateOnNewDocument(() => {
    localStorage.setItem('chat_token', 'mock.jwt.token.admin.eyJ1c2VySWQiOiJ1c2VyX2FkbWluXzAwMSIsInVzZXJuYW1lIjoiYWRtaW4iLCJyb2xlIjoiQURNSU4iLCJleHAiOjk5OTk5OTk5OTl9.signature');
    localStorage.setItem('chat_role', 'ADMIN');
    localStorage.setItem('chat_username', 'admin');
    localStorage.setItem('chat_my_id', 'user_admin_001');
    window.alert = (msg) => { console.log('Suppressed alert:', msg); };
  });

  async function takeShot(filename, description) {
    const filePath = path.join(SCREENSHOT_DIR, filename);
    await page.screenshot({ path: filePath, fullPage: false });
    console.log(`✅ [CAPTURED] ${filename}: ${description}`);
  }

  try {
    // ═══════════════════════════════════════════════════════════════════════════
    // ADMIN DASHBOARD TABS
    // ═══════════════════════════════════════════════════════════════════════════
    console.log('\n--- Navigating to Admin Dashboard ---');
    await page.goto('http://localhost:8080/admin.html', { waitUntil: 'domcontentloaded' });
    await sleep(2500);

    // HÌNH 4.9: Admin Dashboard - Real-time Metrics
    console.log('\n--- Capturing Hình 4.9 (Admin Dashboard) ---');
    await page.evaluate(() => {
      switchTab('dashboard');
      if (typeof lucide !== 'undefined') lucide.createIcons();
    });
    await sleep(2500);
    await takeShot('Hinh_4.9_Admin_Dashboard_Realtime_Metrics.png', 'Giao diện Admin Dashboard - Real-time Metrics');

    // HÌNH 4.10: Quản lý Người dùng và Phân quyền RBAC
    console.log('\n--- Capturing Hình 4.10 (Admin Users RBAC) ---');
    await page.evaluate(() => {
      switchTab('users');
      if (typeof lucide !== 'undefined') lucide.createIcons();
    });
    await sleep(1500);
    await takeShot('Hinh_4.10_Admin_Quan_Ly_Nguoi_Dung_RBAC.png', 'Giao diện Quản lý Người dùng và Phân quyền RBAC');

    // HÌNH 4.11: Security Firewall - Blacklist IP & Maintenance Mode
    console.log('\n--- Capturing Hình 4.11 (Security Firewall) ---');
    await page.evaluate(() => {
      switchTab('security');
      const mToggle = document.getElementById('maintenanceToggle');
      if (mToggle) mToggle.checked = true;
      const mBadge = document.getElementById('maintenanceBadge');
      if (mBadge) {
        mBadge.style.color = '#f59e0b';
        mBadge.innerHTML = 'TRẠNG THÁI: <span style="color:#f59e0b;font-weight:900;">ĐANG BẬT BẢO TRÌ (CHỈ ADMIN TRUY CẬP)</span>';
      }
      if (typeof lucide !== 'undefined') lucide.createIcons();
    });
    await sleep(1500);
    await takeShot('Hinh_4.11_Security_Firewall_Blacklist_IP_Maintenance.png', 'Giao diện Security Firewall - Blacklist IP & Maintenance Mode');

    // HÌNH 4.12: Thống kê Throughput và Top Users
    console.log('\n--- Capturing Hình 4.12 (Stats Throughput & Top Users) ---');
    await page.evaluate(() => {
      switchTab('stats');
      if (typeof lucide !== 'undefined') lucide.createIcons();
    });
    await sleep(2000);
    await takeShot('Hinh_4.12_Thong_Ke_Throughput_Va_Top_Users.png', 'Giao diện Thống kê Throughput và Top Users');

    // HÌNH 4.13a: Admin Broadcast Composer
    console.log('\n--- Capturing Hình 4.13a & 4.13 (Admin Broadcast Composer) ---');
    await page.evaluate(() => {
      switchTab('broadcast');
      document.getElementById('broadcastType').value = 'URGENT';
      document.getElementById('broadcastMsgInput').value = 'Hệ thống sẽ tiến hành bảo trì nâng cấp cụm Cassandra vào lúc 00:00 ngày 07/10/2026. Vui lòng lưu lại dữ liệu hội thoại quan trọng!';
      if (typeof lucide !== 'undefined') lucide.createIcons();
    });
    await sleep(1000);
    await takeShot('Hinh_4.13a_Broadcast_Admin_Composer.png', 'Giao diện Admin Soạn thông báo Broadcast khẩn cấp');
    await takeShot('Hinh_4.13_Broadcast_Thong_Bao_Khan_Cap.png', 'Giao diện Broadcast thông báo khẩn cấp toàn hệ thống');

    // HÌNH 4.14: WebSocket Gateway Logs và Session Management
    console.log('\n--- Capturing Hình 4.14 (WebSocket Gateway Logs & Sessions) ---');
    await page.evaluate(() => {
      switchTab('online-users');
      if (typeof lucide !== 'undefined') lucide.createIcons();
    });
    await sleep(2000);
    await takeShot('Hinh_4.14_WebSocket_Gateway_Logs_Session_Management.png', 'Giao diện WebSocket Gateway Logs và Session Management');

    console.log('\n🎉 ALL ADMIN SCREENSHOTS SUCCESSFULLY CAPTURED!');

  } catch (err) {
    console.error('❌ Error during capture:', err);
  } finally {
    await browser.close();
  }
}

run();
