const puppeteer = require('C:/Users/duy/node_modules/puppeteer-core');
const path = require('path');

const ADMIN_JWT = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.' +
  Buffer.from(JSON.stringify({ sub: 'admin', role: 'ADMIN', exp: Math.floor(Date.now() / 1000) + 86400 * 30 })).toString('base64url') +
  '.mock_signature_for_automation_testing_only';

(async () => {
    const browser = await puppeteer.launch({
        executablePath: 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',
        headless: 'new',
        args: [
            '--no-sandbox',
            '--disable-setuid-sandbox',
            '--window-size=1920,1080',
            '--use-fake-ui-for-media-stream',
            '--use-fake-device-for-media-stream'
        ]
    });
    const page = await browser.newPage();
    await page.setViewport({ width: 1920, height: 1080, deviceScaleFactor: 1.25 });

    await page.goto('http://localhost:8080/index.html', { waitUntil: 'networkidle0' });

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
        selectConversation('conv_direct_nam', 'Hoàng Nam', 'user_nam_002', 'DIRECT', '');
    }, ADMIN_JWT);

    await new Promise(r => setTimeout(r, 1500));

    // Open AR modal cleanly without error toast
    await page.evaluate(async () => {
        // mock getUserMedia
        if (!navigator.mediaDevices) navigator.mediaDevices = {};
        navigator.mediaDevices.getUserMedia = () => Promise.resolve(new MediaStream());
        
        const modal = document.getElementById('arCameraModal');
        modal.classList.add('show');
        
        // Fetch memes if needed
        try {
            const r = await fetch('/api/memes');
            if (r.ok) localMemeCache = await r.json();
        } catch (e) {}

        simulateARFaceDetection('HAPPY');
        document.querySelectorAll('.toast').forEach(t => t.remove());
    });

    await new Promise(r => setTimeout(r, 1000));
    await page.evaluate(() => {
        document.querySelectorAll('.toast').forEach(t => t.remove());
    });

    const targetPath = path.join(__dirname, 'screenshots', 'Hinh_4.4_AR_Meme_Matching_Webcam.png');
    await page.screenshot({ path: targetPath });
    console.log('✅ Captured clean Hinh 4.4 without error toast to:', targetPath);

    await browser.close();
})();
