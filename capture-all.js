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
  console.log('🚀 Starting Chrome for Automated Screenshot Capture...');
  
  const browser = await puppeteer.launch({
    executablePath: CHROME_PATH,
    headless: 'new',
    defaultViewport: {
      width: 1920,
      height: 1080,
      deviceScaleFactor: 1.25 // Ultra-crisp HD rendering for thesis reports
    },
    args: [
      '--no-sandbox',
      '--disable-setuid-sandbox',
      '--disable-gpu',
      '--disable-dev-shm-usage',
      '--window-size=1920,1080',
      '--use-fake-ui-for-media-stream',
      '--use-fake-device-for-media-stream'
    ]
  });

  const page = await browser.newPage();

  // Helper to save screenshot
  async function takeShot(filename, description) {
    const filePath = path.join(SCREENSHOT_DIR, filename);
    await page.screenshot({ path: filePath, fullPage: false });
    console.log(`✅ [CAPTURED] ${filename}: ${description}`);
  }

  try {
    // ═══════════════════════════════════════════════════════════════════════════
    // HÌNH 4.6: Giao diện Đăng nhập và xác thực JWT Token
    // ═══════════════════════════════════════════════════════════════════════════
    console.log('\n--- Capturing Hình 4.6 (Đăng nhập) ---');
    await page.goto('http://localhost:8080/index.html', { waitUntil: 'networkidle0' });
    await page.evaluate(() => {
      localStorage.clear();
      if (typeof logout === 'function') logout();
      document.getElementById('loginUsername').value = 'admin';
      document.getElementById('loginPassword').value = 'AdminSecure@2026';
      switchTab('login');
      if (typeof lucide !== 'undefined') lucide.createIcons();
    });
    await sleep(1500);
    await takeShot('Hinh_4.6_Dang_Nhap_Va_Xac_Thuc_JWT.png', 'Giao diện Đăng nhập và xác thực JWT Token');

    // ═══════════════════════════════════════════════════════════════════════════
    // HÌNH 4.7: Giao diện Đăng ký tài khoản thành viên mới
    // ═══════════════════════════════════════════════════════════════════════════
    console.log('\n--- Capturing Hình 4.7 (Đăng ký) ---');
    await page.evaluate(() => {
      switchTab('register');
      document.getElementById('regUsername').value = 'nguyen_van_a';
      document.getElementById('regEmail').value = 'nguyenvana@chatapp.com';
      document.getElementById('regPassword').value = 'Password123@Secure';
      if (typeof lucide !== 'undefined') lucide.createIcons();
    });
    await sleep(1000);
    await takeShot('Hinh_4.7_Dang_Ky_Tai_Khoan_Moi.png', 'Giao diện Đăng ký tài khoản thành viên mới');

    // ═══════════════════════════════════════════════════════════════════════════
    // HÌNH 4.1: Giao diện Trang chủ hệ thống Chat Platform
    // ═══════════════════════════════════════════════════════════════════════════
    console.log('\n--- Capturing Hình 4.1 (Trang chủ) ---');
    await page.evaluate(() => {
      onAuthSuccess({
        accessToken: "mock.jwt.token.admin.eyJ1c2VySWQiOiJ1c2VyX2FkbWluXzAwMSIsInVzZXJuYW1lIjoiYWRtaW4iLCJyb2xlIjoiQURNSU4iLCJleHAiOjk5OTk5OTk5OTl9.signature",
        userId: "user_admin_001",
        username: "admin",
        role: "ADMIN"
      });
      // Show empty companion state initially for main landing view
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
    // HÌNH 4.2: Giao diện Chat 1-1 với giao diện 3D Glassmorphism
    // ═══════════════════════════════════════════════════════════════════════════
    console.log('\n--- Capturing Hình 4.2 (Chat 1-1) ---');
    await page.evaluate(async () => {
      await selectConversation('conv_direct_nam', 'Hoàng Nam', 'user_nam_002', 'DIRECT', '');
      if (typeof lucide !== 'undefined') lucide.createIcons();
    });
    await sleep(1500);
    await takeShot('Hinh_4.2_Chat_1-1_3D_Glassmorphism.png', 'Giao diện Chat 1-1 với giao diện 3D Glassmorphism');

    // ═══════════════════════════════════════════════════════════════════════════
    // HÌNH 4.3: Giao diện Chat nhóm đa thành viên
    // ═══════════════════════════════════════════════════════════════════════════
    console.log('\n--- Capturing Hình 4.3 (Chat nhóm đa thành viên) ---');
    await page.evaluate(async () => {
      await selectConversation('conv_group_dev', 'Dev Team 2026 - Microservices Project', '', 'GROUP', '');
      // Enhance header subtitle with member list
      document.querySelector('.hstatus').textContent = '5 thành viên • Trực tuyến: Hoàng Nam, Lê Mai, Trần Dũng, Admin, Nguyễn An';
      if (typeof lucide !== 'undefined') lucide.createIcons();
    });
    await sleep(1500);
    await takeShot('Hinh_4.3_Chat_Nhom_Da_Thanh_Vien.png', 'Giao diện Chat nhóm đa thành viên');

    // ═══════════════════════════════════════════════════════════════════════════
    // HÌNH 4.4: Giao diện Tính năng AR Meme Matching qua Webcam
    // ═══════════════════════════════════════════════════════════════════════════
    console.log('\n--- Capturing Hình 4.4 (AR Meme Matching) ---');
    await page.evaluate(async () => {
      await openARCamera();
      simulateARFaceDetection('HAPPY');
      if (typeof lucide !== 'undefined') lucide.createIcons();
    });
    await sleep(1200);
    await takeShot('Hinh_4.4_AR_Meme_Matching_Webcam.png', 'Giao diện Tính năng AR Meme Matching qua Webcam');
    await page.evaluate(() => closeARCamera());
    await sleep(500);

    // ═══════════════════════════════════════════════════════════════════════════
    // HÌNH 4.5: Giao diện Thả Emoji Reactions và Thu hồi tin nhắn
    // ═══════════════════════════════════════════════════════════════════════════
    console.log('\n--- Capturing Hình 4.5 (Emoji Reactions & Thu hồi tin nhắn) ---');
    await page.evaluate(async () => {
      await selectConversation('conv_direct_nam', 'Hoàng Nam', 'user_nam_002', 'DIRECT', '');
      
      // Ensure reaction chips are populated on message msg_nam_03
      const reactBox = document.getElementById('reactions-msg_nam_03');
      if (reactBox) {
        reactBox.innerHTML = `
          <span class="reaction-badge" style="background: rgba(0, 242, 254, 0.15); border: 1px solid #00f2fe; color: #fff; font-weight: 700; padding: 4px 10px; border-radius: 20px;">👍 3</span>
          <span class="reaction-badge" style="background: rgba(244, 63, 94, 0.15); border: 1px solid #f43f5e; color: #fff; font-weight: 700; padding: 4px 10px; border-radius: 20px;">❤️ 5</span>
          <span class="reaction-badge" style="background: rgba(245, 158, 11, 0.15); border: 1px solid #f59e0b; color: #fff; font-weight: 700; padding: 4px 10px; border-radius: 20px;">😂 2</span>
        `;
      }
      
      // Ensure recalled message is rendered clearly with distinctive style
      const bubbleRecall = document.getElementById('msg-bubble-msg_admin_04_recalled');
      if (bubbleRecall) {
        bubbleRecall.style.border = '1px dashed rgba(244, 63, 94, 0.4)';
        bubbleRecall.style.background = 'rgba(244, 63, 94, 0.08)';
        bubbleRecall.innerHTML = '<span style="font-style: italic; color: #fb7185; display: inline-flex; align-items: center; gap: 8px; font-weight: 600;"><i data-lucide="ban" style="width: 16px; height: 16px; color: #f43f5e;"></i> Tin nhắn này đã được thu hồi</span>';
      }

      if (typeof lucide !== 'undefined') lucide.createIcons();
    });
    await sleep(1000);
    await takeShot('Hinh_4.5_Emoji_Reactions_Va_Thu_Hoi_Tin_Nhan.png', 'Giao diện Thả Emoji Reactions và Thu hồi tin nhắn');

    // ═══════════════════════════════════════════════════════════════════════════
    // HÌNH 4.8: Giao diện Upload và chia sẻ ảnh qua MinIO S3
    // ═══════════════════════════════════════════════════════════════════════════
    console.log('\n--- Capturing Hình 4.8 (Upload ảnh qua MinIO S3) ---');
    await page.evaluate(() => {
      // Scroll to view the MinIO uploaded image bubble prominently
      const targetRow = document.getElementById('msg-row-msg_admin_05_minio');
      if (targetRow) {
        targetRow.scrollIntoView({ behavior: 'instant', block: 'center' });
      }
    });
    await sleep(1000);
    await takeShot('Hinh_4.8_Upload_Va_Chia_Se_Anh_MinIO_S3.png', 'Giao diện Upload và chia sẻ ảnh qua MinIO S3');

    // ═══════════════════════════════════════════════════════════════════════════
    // HÌNH 4.13b: Banner Broadcast hiển thị trên client
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
    // ADMIN DASHBOARD TABS
    // ═══════════════════════════════════════════════════════════════════════════
    console.log('\n--- Navigating to Admin Dashboard ---');
    await page.goto('http://localhost:8080/admin.html', { waitUntil: 'networkidle0' });
    await sleep(1500);

    // HÌNH 4.9: Admin Dashboard - Real-time Metrics
    console.log('\n--- Capturing Hình 4.9 (Admin Dashboard) ---');
    await page.evaluate(() => {
      switchTab('dashboard');
      if (typeof lucide !== 'undefined') lucide.createIcons();
    });
    await sleep(2000);
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
      // Set maintenance toggle visual
      const mToggle = document.getElementById('maintenanceToggle');
      if (mToggle) mToggle.checked = true;
      const mBadge = document.getElementById('maintenanceBadge');
      if (mBadge) {
        mBadge.style.color = 'var(--warning)';
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
    await sleep(1500);
    await takeShot('Hinh_4.12_Thong_Ke_Throughput_Va_Top_Users.png', 'Giao diện Thống kê Throughput và Top Users');

    // HÌNH 4.13a: Admin Broadcast Composer
    console.log('\n--- Capturing Hình 4.13a (Admin Broadcast Composer) ---');
    await page.evaluate(() => {
      switchTab('broadcast');
      document.getElementById('broadcastType').value = 'URGENT';
      document.getElementById('broadcastMsgInput').value = 'Hệ thống sẽ tiến hành bảo trì nâng cấp cụm Cassandra vào lúc 00:00 ngày 07/10/2026. Vui lòng lưu lại dữ liệu hội thoại quan trọng!';
      if (typeof lucide !== 'undefined') lucide.createIcons();
    });
    await sleep(1000);
    await takeShot('Hinh_4.13a_Broadcast_Admin_Composer.png', 'Giao diện Admin Soạn thông báo Broadcast khẩn cấp');
    
    // Also save as Hinh_4.13_Broadcast_Thong_Bao_Khan_Cap.png (admin view) for default STT 13
    await takeShot('Hinh_4.13_Broadcast_Thong_Bao_Khan_Cap.png', 'Giao diện Broadcast thông báo khẩn cấp toàn hệ thống (Admin)');

    // HÌNH 4.14: WebSocket Gateway Logs và Session Management
    console.log('\n--- Capturing Hình 4.14 (WebSocket Gateway Logs & Sessions) ---');
    await page.evaluate(() => {
      switchTab('online-users');
      if (typeof lucide !== 'undefined') lucide.createIcons();
    });
    await sleep(1500);
    await takeShot('Hinh_4.14_WebSocket_Gateway_Logs_Session_Management.png', 'Giao diện WebSocket Gateway Logs và Session Management');

    console.log('\n🎉 ALL 14 SCREENSHOTS SUCCESSFULLY CAPTURED!');

  } catch (err) {
    console.error('❌ Error during capture:', err);
  } finally {
    await browser.close();
  }
}

run();
