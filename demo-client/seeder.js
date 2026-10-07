const fs = require('fs');

// Configuration
const AUTH_URL = 'http://localhost:8081/api/auth';
const CHAT_URL = 'http://localhost:8082/api';
const NUM_USERS = 50;
const NUM_MESSAGES = 5000;
const BYPASS_HEADER = { 'X-Bypass-Rate-Limit': 'load-test-secret-123' };

let users = []; // Array of { id, username, token }
let groupId = null;

async function request(url, method, body, token = null) {
  const headers = { 
    'Content-Type': 'application/json',
    ...BYPASS_HEADER
  };
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }
  
  const options = { method, headers };
  if (body) options.body = JSON.stringify(body);

  const res = await fetch(url, options);
  const isJson = res.headers.get('content-type')?.includes('application/json');
  const data = isJson ? await res.json() : await res.text();
  
  if (!res.ok) {
    throw new Error(`API Error: ${res.status} ${res.statusText} - ${JSON.stringify(data)}`);
  }
  return data;
}

async function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

async function runSeeder() {
  console.log(`🚀 BẮT ĐẦU CHẠY SEEDER: ${NUM_USERS} users, ${NUM_MESSAGES} messages`);
  
  // 1. Tạo và đăng nhập Users
  console.log(`\n--- 1. Tạo và đăng nhập ${NUM_USERS} users ---`);
  for (let i = 1; i <= NUM_USERS; i++) {
    const username = `testuser_${i}`;
    const password = `password123`;
    
    // Register (ignoring if already exists)
    try {
      await request(`${AUTH_URL}/register`, 'POST', { username, password, email: `${username}@test.com` });
    } catch (e) {
      // Ignored - user might exist
    }

    // Login
    try {
      const loginRes = await request(`${AUTH_URL}/login`, 'POST', { usernameOrEmail: username, password });
      users.push({
        id: loginRes.userId,
        username,
        token: loginRes.accessToken
      });
      process.stdout.write('.');
    } catch (e) {
      console.error(`\nLỗi đăng nhập user ${username}:`, e.message);
    }
  }
  console.log(`\nĐã khởi tạo xong ${users.length} users.`);

  if (users.length === 0) return;

  // 2. Tạo Group Chat chung cho tất cả
  console.log(`\n--- 2. Tạo Group Chat ---`);
  const adminUser = users[0];
  const memberIds = users.map(u => u.id);
  try {
    const groupRes = await request(`${CHAT_URL}/conversations/group`, 'POST', {
      type: 'GROUP',
      name: 'Phòng Load Test Khổng Lồ',
      memberUserIds: memberIds
    }, adminUser.token);
    groupId = groupRes.id;
    console.log(`Đã tạo Group ID: ${groupId}`);
  } catch (e) {
    console.error('Lỗi tạo Group:', e.message);
    return;
  }

  // 3. Bắn tin nhắn liên tục
  console.log(`\n--- 3. Bắn ${NUM_MESSAGES} tin nhắn vào Group ---`);
  const startTime = Date.now();
  let successCount = 0;
  
  // Chúng ta sẽ chạy song song (batch) để tạo tải lớn hơn, thay vì tuần tự
  const BATCH_SIZE = 100;
  for (let i = 0; i < NUM_MESSAGES; i += BATCH_SIZE) {
    const batchPromises = [];
    const currentBatchSize = Math.min(BATCH_SIZE, NUM_MESSAGES - i);
    
    for (let j = 0; j < currentBatchSize; j++) {
      const msgIndex = i + j + 1;
      const randomUser = users[Math.floor(Math.random() * users.length)];
      
      const p = request(`${CHAT_URL}/messages`, 'POST', {
        conversationId: groupId,
        content: `Test Message #${msgIndex} from ${randomUser.username}`,
        replyToId: null,
        mediaUrls: []
      }, randomUser.token)
      .then(() => successCount++)
      .catch(e => console.error(`\nLỗi gửi tin #${msgIndex}:`, e.message));
      
      batchPromises.push(p);
    }
    
    await Promise.all(batchPromises);
    process.stdout.write(`\rĐã gửi: ${successCount}/${NUM_MESSAGES} (Batch size: ${BATCH_SIZE})`);
  }

  const endTime = Date.now();
  const durationSec = ((endTime - startTime) / 1000).toFixed(2);
  const rps = (successCount / durationSec).toFixed(2);

  console.log(`\n\n✅ HOÀN TẤT LOAD TEST`);
  console.log(`- Thời gian: ${durationSec}s`);
  console.log(`- Tốc độ: ${rps} request/giây`);
}

runSeeder().catch(console.error);
