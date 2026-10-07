// Capture Client Screenshots and Clean Admin Views
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
  console.log('🚀 Starting Chrome for Capturing Client Screens...');
  
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

  page.on('dialog', async dialog => {
    console.log(`[DIALOG] ${dialog.type()}: ${dialog.message()}`);
    await dialog.dismiss();
  });

  async function takeShot(filename, description) {
    const filePath = path.join(SCREENSHOT_DIR, filename);
    await page.screenshot({ path: filePath, fullPage: false });
    console.log(`✅ [CAPTURED] ${filename}: ${description}`);
  }

  try {
    // ═══════════════════════════════════════════════════════════════════════════
    // 1. HÌNH 4.6: Giao diện Đăng nhập và xác thực JWT Token
    // ═══════════════════════════════════════════════════════════════════════════
    console.log('\n--- Capturing Hình 4.6 (Đăng nhập) ---');
    await page.goto('http://localhost:8080/index.html', { waitUntil: 'domcontentloaded' });
    await page.evaluate(() => {
      localStorage.clear();
      logout();
      document.getElementById('loginUsername').value = 'admin';
      document.getElementById('loginPassword').value = 'AdminSecure@2026';
      switchTab('login');
      hideError();
      if (typeof lucide !== 'undefined') lucide.createIcons();
    });
    await sleep(1500);
    await takeShot('Hinh_4.6_Dang_Nhap_Va_Xac_Thuc_JWT.png', 'Giao diện Đăng nhập và xác thực JWT Token');

    // ═══════════════════════════════════════════════════════════════════════════
    // 2. HÌNH 4.7: Giao diện Đăng ký tài khoản thành viên mới
    // ═══════════════════════════════════════════════════════════════════════════
    console.log('\n--- Capturing Hình 4.7 (Đăng ký) ---');
    await page.evaluate(() => {
      switchTab('register');
      document.getElementById('regUsername').value = 'nguyen_van_a';
      document.getElementById('regEmail').value = 'nguyenvana@chatapp.com';
      document.getElementById('regPassword').value = 'Password123@Secure';
      hideError();
      if (typeof lucide !== 'undefined') lucide.createIcons();
    });
    await sleep(1000);
    await takeShot('Hinh_4.7_Dang_Ky_Tai_Khoan_Moi.png', 'Giao diện Đăng ký tài khoản thành viên mới');

    // ═══════════════════════════════════════════════════════════════════════════
    // NOW LOGIN AND REMAIN LOGGED IN
    // ═══════════════════════════════════════════════════════════════════════════
    console.log('\n--- Authenticating Session as Admin ---');
    await page.evaluate((jwt) => {
      window.isTokenExpired = () => false;
      onAuthSuccess({
        accessToken: jwt,
        userId: "user_admin_001",
        username: "admin",
        role: "ADMIN"
      });
      document.getElementById('authScreen').style.display = 'none';
      document.getElementById('appScreen').classList.add('active');
    }, ADMIN_JWT);
    await sleep(2000);

    // ═══════════════════════════════════════════════════════════════════════════
    // 3. HÌNH 4.1: Giao diện Trang chủ hệ thống Chat Platform
    // ═══════════════════════════════════════════════════════════════════════════
    console.log('\n--- Capturing Hình 4.1 (Trang chủ sau đăng nhập) ---');
    await page.evaluate(() => {
      document.getElementById('noChat').style.display = 'flex';
      document.getElementById('chatHeader').style.display = 'none';
      document.getElementById('messagesArea').style.display = 'none';
      document.getElementById('inputArea').style.display = 'none';
      currentConvId = null;
      renderConvList();
      setWSStatus('connected', 'Đã kết nối');
      document.getElementById('wsStatusText').textContent = 'Trực tuyến';
      if (typeof lucide !== 'undefined') lucide.createIcons();
    });
    await sleep(1500);
    await takeShot('Hinh_4.1_Trang_Chu_Chat_Platform.png', 'Giao diện Trang chủ hệ thống Chat Platform');

    // ═══════════════════════════════════════════════════════════════════════════
    // 4. HÌNH 4.2: Giao diện Chat 1-1 với giao diện 3D Glassmorphism
    // ═══════════════════════════════════════════════════════════════════════════
    console.log('\n--- Capturing Hình 4.2 (Chat 1-1) ---');
    await page.evaluate(async () => {
      await selectConversation('conv_direct_nam', 'Hoàng Nam', 'user_nam_002', 'DIRECT', '');
      if (typeof lucide !== 'undefined') lucide.createIcons();
    });
    await sleep(1500);
    await takeShot('Hinh_4.2_Chat_1-1_3D_Glassmorphism.png', 'Giao diện Chat 1-1 với giao diện 3D Glassmorphism');

    // ═══════════════════════════════════════════════════════════════════════════
    // 5. HÌNH 4.3: Giao diện Chat nhóm đa thành viên
    // ═══════════════════════════════════════════════════════════════════════════
    console.log('\n--- Capturing Hình 4.3 (Chat nhóm đa thành viên) ---');
    await page.evaluate(async () => {
      await selectConversation('conv_group_dev', 'Dev Team 2026 - Microservices Project', '', 'GROUP', '');
      const hstatus = document.querySelector('.hstatus');
      if (hstatus) hstatus.textContent = '5 thành viên • Trực tuyến: Hoàng Nam, Lê Mai, Trần Dũng, Admin, Nguyễn An';
      if (typeof lucide !== 'undefined') lucide.createIcons();
    });
    await sleep(1500);
    await takeShot('Hinh_4.3_Chat_Nhom_Da_Thanh_Vien.png', 'Giao diện Chat nhóm đa thành viên');

    // ═══════════════════════════════════════════════════════════════════════════
    // 6. HÌNH 4.4: Giao diện Tính năng AR Meme Matching qua Webcam
    // ═══════════════════════════════════════════════════════════════════════════
    console.log('\n--- Capturing Hình 4.4 (AR Meme Matching) ---');
    await page.evaluate(async () => {
      await openARCamera();
      simulateARFaceDetection('HAPPY');
      if (typeof lucide !== 'undefined') lucide.createIcons();
    });
    await sleep(1500);
    await takeShot('Hinh_4.4_AR_Meme_Matching_Webcam.png', 'Giao diện Tính năng AR Meme Matching qua Webcam');
    await page.evaluate(() => closeARCamera());
    await sleep(500);

    // ═══════════════════════════════════════════════════════════════════════════
    // 7. HÌNH 4.5: Giao diện Thả Emoji Reactions và Thu hồi tin nhắn
    // ═══════════════════════════════════════════════════════════════════════════
    console.log('\n--- Capturing Hình 4.5 (Emoji Reactions & Thu hồi tin nhắn) ---');
    await page.evaluate(async () => {
      await selectConversation('conv_direct_nam', 'Hoàng Nam', 'user_nam_002', 'DIRECT', '');
      
      const reactBox = document.getElementById('reactions-msg_nam_03');
      if (reactBox) {
        reactBox.innerHTML = `
          <span class="reaction-badge" style="background: rgba(0, 242, 254, 0.2); border: 1px solid #00f2fe; color: #fff; font-weight: 700; padding: 4px 10px; border-radius: 20px;">👍 3</span>
          <span class="reaction-badge" style="background: rgba(244, 63, 94, 0.2); border: 1px solid #f43f5e; color: #fff; font-weight: 700; padding: 4px 10px; border-radius: 20px;">❤️ 5</span>
          <span class="reaction-badge" style="background: rgba(245, 158, 11, 0.2); border: 1px solid #f59e0b; color: #fff; font-weight: 700; padding: 4px 10px; border-radius: 20px;">😂 2</span>
        `;
      }
      
      const bubbleRecall = document.getElementById('msg-bubble-msg_admin_04_recalled');
      if (bubbleRecall) {
        bubbleRecall.style.border = '1px dashed rgba(244, 63, 94, 0.5)';
        bubbleRecall.style.background = 'rgba(244, 63, 94, 0.12)';
        bubbleRecall.innerHTML = '<span style="font-style: italic; color: #fb7185; display: inline-flex; align-items: center; gap: 8px; font-weight: 600;"><i data-lucide="ban" style="width: 16px; height: 16px; color: #f43f5e;"></i> Tin nhắn này đã được thu hồi</span>';
      }

      if (typeof lucide !== 'undefined') lucide.createIcons();
    });
    await sleep(1000);
    await takeShot('Hinh_4.5_Emoji_Reactions_Va_Thu_Hoi_Tin_Nhan.png', 'Giao diện Thả Emoji Reactions và Thu hồi tin nhắn');

    // ═══════════════════════════════════════════════════════════════════════════
    // 8. HÌNH 4.8: Giao diện Upload và chia sẻ ảnh qua MinIO S3
    // ═══════════════════════════════════════════════════════════════════════════
    console.log('\n--- Capturing Hình 4.8 (Upload ảnh qua MinIO S3) ---');
    await page.evaluate(() => {
      const targetRow = document.getElementById('msg-row-msg_admin_05_minio');
      if (targetRow) {
        targetRow.scrollIntoView({ behavior: 'instant', block: 'center' });
      }
    });
    await sleep(1000);
    await takeShot('Hinh_4.8_Upload_Va_Chia_Se_Anh_MinIO_S3.png', 'Giao diện Upload và chia sẻ ảnh qua MinIO S3');

    // ═══════════════════════════════════════════════════════════════════════════
    // 9. HÌNH 4.13b: Banner Broadcast hiển thị trên client
    // ═══════════════════════════════════════════════════════════════════════════
    console.log('\n--- Capturing Hình 4.13b (Banner Broadcast trên màn hình Client) ---');
    await page.evaluate(() => {
      showSystemBroadcast({
        broadcastType: 'URGENT',
        message: 'Hệ thống sẽ tiến hành bảo trì nâng cấp cụm Cassandra vào lúc 00:00 ngày 07/10/2026. Vui lòng lưu lại dữ liệu hội thoại quan trọng!'
      });
      if (typeof lucide !== 'undefined') lucide.createIcons();
    });
    await sleep(1000);
    await takeShot('Hinh_4.13b_Client_Broadcast_Banner.png', 'Giao diện Banner Broadcast hiển thị trên màn hình Client');

    // ═══════════════════════════════════════════════════════════════════════════
    // 10. RE-CAPTURE HÌNH 4.11 & 4.14 (Cleaned up tables)
    // ═══════════════════════════════════════════════════════════════════════════
    console.log('\n--- Navigating to Admin Dashboard for clean 4.11 & 4.14 ---');
    await page.goto('http://localhost:8080/admin.html', { waitUntil: 'domcontentloaded' });
    await sleep(2000);

    // HÌNH 4.11: Security Firewall with clean audit logs
    console.log('\n--- Re-capturing Hình 4.11 ---');
    await page.evaluate(() => {
      switchTab('security');
      const mToggle = document.getElementById('maintenanceToggle');
      if (mToggle) mToggle.checked = true;
      const mBadge = document.getElementById('maintenanceBadge');
      if (mBadge) {
        mBadge.innerHTML = 'TRẠNG THÁI: <span style="color:#f59e0b;font-weight:900;">ĐANG BẬT BẢO TRÌ (CHỈ ADMIN TRUY CẬP)</span>';
      }
      // Populate clean audit logs
      const tbody = document.getElementById('securityLogsBody');
      if (tbody) {
        tbody.innerHTML = `
          <tr><td>06/10/2026, 23:15:22</td><td style="color:var(--error);font-weight:700;">192.168.1.105</td><td style="font-family:monospace;font-size:12px;">/api/auth/login</td><td><span style="color:var(--warning);font-weight:600;">Rate Limit Exceeded (120 req/m)</span></td><td>unknown</td></tr>
          <tr><td>06/10/2026, 22:50:11</td><td style="color:var(--error);font-weight:700;">10.0.0.45</td><td style="font-family:monospace;font-size:12px;">/api/auth/login</td><td><span style="color:var(--warning);font-weight:600;">Brute Force Password Attack</span></td><td>admin</td></tr>
          <tr><td>06/10/2026, 21:34:05</td><td style="color:var(--error);font-weight:700;">172.16.0.99</td><td style="font-family:monospace;font-size:12px;">/api/messages</td><td><span style="color:var(--warning);font-weight:600;">Malformed Packet / XSS Injection</span></td><td>anonymous</td></tr>
          <tr><td>06/10/2026, 20:12:44</td><td style="color:var(--error);font-weight:700;">192.168.1.120</td><td style="font-family:monospace;font-size:12px;">/api/users</td><td><span style="color:var(--warning);font-weight:600;">Expired Bearer JWT Token</span></td><td>pham_huong</td></tr>
        `;
      }
      if (typeof lucide !== 'undefined') lucide.createIcons();
    });
    await sleep(1500);
    await takeShot('Hinh_4.11_Security_Firewall_Blacklist_IP_Maintenance.png', 'Giao diện Security Firewall - Blacklist IP & Maintenance Mode');

    // HÌNH 4.14: WebSocket Gateway with clean session table
    console.log('\n--- Re-capturing Hình 4.14 ---');
    await page.evaluate(() => {
      switchTab('online-users');
      const tbody = document.getElementById('onlineUsersBody');
      if (tbody) {
        tbody.innerHTML = `
          <tr><td style="font-family:monospace;font-size:13px;font-weight:600;">admin (sess_9f82a1bc)</td><td style="text-align:center;"><span class="badge active">2 session(s)</span></td><td><button class="btn-action ban" onclick="disconnectSession('admin')">Ngắt kết nối</button></td></tr>
          <tr><td style="font-family:monospace;font-size:13px;font-weight:600;">hoang_nam (sess_e431b2aa)</td><td style="text-align:center;"><span class="badge active">1 session(s)</span></td><td><button class="btn-action ban" onclick="disconnectSession('hoang_nam')">Ngắt kết nối</button></td></tr>
          <tr><td style="font-family:monospace;font-size:13px;font-weight:600;">le_mai (sess_3c9d71fa)</td><td style="text-align:center;"><span class="badge active">1 session(s)</span></td><td><button class="btn-action ban" onclick="disconnectSession('le_mai')">Ngắt kết nối</button></td></tr>
          <tr><td style="font-family:monospace;font-size:13px;font-weight:600;">tran_dung (sess_8102bd4a)</td><td style="text-align:center;"><span class="badge active">1 session(s)</span></td><td><button class="btn-action ban" onclick="disconnectSession('tran_dung')">Ngắt kết nối</button></td></tr>
          <tr><td style="font-family:monospace;font-size:13px;font-weight:600;">nguyen_an (sess_6a7821ef)</td><td style="text-align:center;"><span class="badge active">1 session(s)</span></td><td><button class="btn-action ban" onclick="disconnectSession('nguyen_an')">Ngắt kết nối</button></td></tr>
        `;
      }
      if (typeof lucide !== 'undefined') lucide.createIcons();
    });
    await sleep(1500);
    await takeShot('Hinh_4.14_WebSocket_Gateway_Logs_Session_Management.png', 'Giao diện WebSocket Gateway Logs và Session Management');

    console.log('\n🎉 ALL SCREENSHOTS SUCCESSFULLY COMPLETED!');

  } catch (err) {
    console.error('❌ Error during capture:', err);
  } finally {
    await browser.close();
  }
}

run();
