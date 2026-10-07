// Standalone Mock Server for ChatApp Real-time Microservices Demo
const http = require('http');
const fs = require('fs');
const path = require('path');
const { WebSocketServer } = require('ws');

const PORT = 8080;
const CLIENT_DIR = path.join(__dirname, 'demo-client');

function makeJwt(userId, username, role) {
  const header = Buffer.from(JSON.stringify({ alg: "HS256", typ: "JWT" })).toString('base64url');
  const payload = Buffer.from(JSON.stringify({
    userId,
    sub: userId,
    username,
    role,
    exp: Math.floor(Date.now() / 1000) + 86400 * 30
  })).toString('base64url');
  return `${header}.${payload}.sig_${userId}`;
}

function getUserIdFromAuth(req) {
  const auth = req.headers['authorization'] || '';
  if (auth.startsWith('Bearer ')) {
    const token = auth.slice(7);
    const parts = token.split('.');
    if (parts.length >= 2) {
      try {
        const payload = JSON.parse(Buffer.from(parts[1], 'base64url').toString('utf8'));
        return payload.userId || payload.sub || 'user_admin_001';
      } catch (e) {}
    }
  }
  return 'user_admin_001';
}

const https = require('https');
// Nạp biến môi trường từ file .env cục bộ (file này không được commit lên Git)
try {
  fs.readFileSync(path.join(__dirname, '.env'), 'utf8').split(/\r?\n/).forEach(line => {
    const m = line.match(/^\s*([\w.]+)\s*=\s*(.*)\s*$/);
    if (m && !process.env[m[1]]) process.env[m[1]] = m[2];
  });
} catch (e) {}
const KIRO_API_KEY = process.env.KIRO_API_KEY || '';
const KIRO_MODEL = process.env.KIRO_MODEL || 'qwen/qwen3.7-flash:free';

function queryXkiro(systemPrompt, userPrompt, maxTokens = 600) {
  if (!KIRO_API_KEY) return Promise.resolve(null);
  return new Promise((resolve) => {
    const payload = JSON.stringify({
      model: KIRO_MODEL,
      messages: [
        { role: 'system', content: systemPrompt },
        { role: 'user', content: userPrompt }
      ],
      temperature: 0.7,
      max_tokens: maxTokens
    });

    const req = https.request('https://api.xkiro.com/v1/chat/completions', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${KIRO_API_KEY}`,
        'Content-Length': Buffer.byteLength(payload)
      },
      timeout: 15000
    }, res => {
      let data = '';
      res.on('data', chunk => data += chunk);
      res.on('end', () => {
        try {
          const parsed = JSON.parse(data);
          if (parsed.choices && parsed.choices[0] && parsed.choices[0].message) {
            resolve(parsed.choices[0].message.content.trim());
          } else {
            resolve(null);
          }
        } catch(e) {
          resolve(null);
        }
      });
    });
    req.on('error', () => resolve(null));
    req.on('timeout', () => { req.destroy(); resolve(null); });
    req.write(payload);
    req.end();
  });
}

const ADMIN_JWT = makeJwt('user_admin_001', 'admin', 'ADMIN');

// Mock data
const mockUsers = [
  { id: "user_admin_001", username: "admin", email: "admin@chatapp.com", role: "ADMIN", banned: false, createdAt: "2026-09-01T08:00:00Z" },
  { id: "00000000-0000-0000-0000-0000000000a1", username: "Gemini AI Copilot", email: "copilot@ai.messenger", role: "BOT", banned: false, createdAt: "2026-09-01T08:00:00Z" },
  { id: "user_nam_002", username: "hoang_nam", email: "nam.hoang@company.com", role: "USER", banned: false, createdAt: "2026-09-05T09:30:00Z" },
  { id: "user_mai_003", username: "le_mai", email: "mai.le@company.com", role: "USER", banned: false, createdAt: "2026-09-10T14:15:00Z" },
  { id: "user_dung_004", username: "tran_dung", email: "dung.tran@company.com", role: "USER", banned: false, createdAt: "2026-09-12T11:00:00Z" },
  { id: "user_an_005", username: "nguyen_an", email: "an.nguyen@company.com", role: "USER", banned: false, createdAt: "2026-09-15T16:20:00Z" },
  { id: "user_huong_006", username: "pham_huong", email: "huong.pham@violation.com", role: "USER", banned: true, createdAt: "2026-09-20T10:00:00Z" },
  { id: "user_linh_007", username: "vu_linh", email: "linh.vu@company.com", role: "USER", banned: false, createdAt: "2026-09-22T08:45:00Z" },
  { id: "user_khoa_008", username: "dang_khoa", email: "khoa.dang@company.com", role: "USER", banned: false, createdAt: "2026-09-25T13:10:00Z" }
];

const mockConvs = [
  {
    id: "conv_ai_copilot",
    type: "DIRECT",
    name: "✨ Gemini AI Copilot",
    memberIds: ["user_admin_001", "00000000-0000-0000-0000-0000000000a1"],
    createdAt: "2026-10-06T15:00:00Z"
  },
  {
    id: "conv_direct_nam",
    type: "DIRECT",
    name: "Hoàng Nam",
    memberIds: ["user_admin_001", "user_nam_002"],
    createdAt: "2026-10-06T14:00:00Z"
  },
  {
    id: "conv_group_dev",
    type: "GROUP",
    name: "Dev Team 2026 - Microservices Project",
    memberIds: ["user_admin_001", "user_nam_002", "user_mai_003", "user_dung_004", "user_an_005"],
    createdAt: "2026-10-06T10:00:00Z"
  },
  {
    id: "conv_direct_mai",
    type: "DIRECT",
    name: "Lê Mai",
    memberIds: ["user_admin_001", "user_mai_003"],
    createdAt: "2026-10-06T09:30:00Z"
  },
  {
    id: "conv_group_ai",
    type: "GROUP",
    name: "Phòng Nghiên Cứu AR Meme AI",
    memberIds: ["user_admin_001", "user_nam_002", "user_mai_003"],
    createdAt: "2026-10-05T16:00:00Z"
  },
  {
    id: "conv_direct_dung",
    type: "DIRECT",
    name: "Trần Dũng",
    memberIds: ["user_admin_001", "user_dung_004"],
    createdAt: "2026-10-05T11:20:00Z"
  }
];

const mockMessages = {
  conv_ai_copilot: [
    {
      messageId: "msg_ai_init_01",
      conversationId: "conv_ai_copilot",
      senderId: "00000000-0000-0000-0000-0000000000a1",
      content: "Xin chào Admin! 👋 Tôi là **Gemini AI Copilot** — trợ lý AI thời gian thực được tích hợp vào không gian làm việc của bạn.\n\nTôi có thể hỗ trợ:\n• 📝 **Tóm tắt cuộc trò chuyện** & lập danh sách việc cần làm\n• ⚡ **Smart Reply** — gợi ý phản hồi nhanh theo ngữ cảnh\n• 🌐 **Dịch tin nhắn đa ngôn ngữ** thời gian thực\n• 🎨 **Sinh ảnh AI nghệ thuật** (/imagine)\n• 💻 **Tư vấn kiến trúc** Spring Boot, Cassandra, Redis & WebSocket.\n\nHãy thử hỏi tôi bất kỳ điều gì hoặc tag `@AI` trong bất kỳ phòng chat nào!",
      status: "DELIVERED",
      createdAt: "2026-10-06T15:00:00Z"
    }
  ],
  conv_direct_nam: [
    {
      messageId: "msg_nam_01",
      conversationId: "conv_direct_nam",
      senderId: "user_nam_002",
      content: "Chào Admin! Hệ thống WebSocket Gateway và Cassandra cluster hôm nay chạy rất mượt 👍",
      status: "DELIVERED",
      createdAt: "2026-10-06T14:10:00Z"
    },
    {
      messageId: "msg_admin_02",
      conversationId: "conv_direct_nam",
      senderId: "user_admin_001",
      content: "Chào Nam! Anh vừa test xong tính năng tải ảnh qua MinIO S3 và mã hóa JWT, latency dưới 5ms 🚀",
      status: "DELIVERED",
      createdAt: "2026-10-06T14:12:00Z"
    },
    {
      messageId: "msg_nam_03",
      conversationId: "conv_direct_nam",
      senderId: "user_nam_002",
      content: "Anh em chuẩn bị demo tính năng AR Meme Matching và WebSocket cho hội đồng phản biện nhé!",
      status: "DELIVERED",
      createdAt: "2026-10-06T14:15:00Z"
    },
    {
      messageId: "msg_admin_04_recalled",
      conversationId: "conv_direct_nam",
      senderId: "user_admin_001",
      content: "Tin nhắn này đã được thu hồi",
      recalled: true,
      status: "DELIVERED",
      createdAt: "2026-10-06T14:18:00Z"
    },
    {
      messageId: "msg_admin_05_minio",
      conversationId: "conv_direct_nam",
      senderId: "user_admin_001",
      content: "Sơ đồ kiến trúc Polyglot Persistence & MinIO S3 lưu trữ media tập trung:",
      mediaUrls: ["assets/images/minio-arch.svg"],
      status: "DELIVERED",
      createdAt: "2026-10-06T14:20:00Z"
    },
    {
      messageId: "msg_nam_06",
      conversationId: "conv_direct_nam",
      senderId: "user_nam_002",
      content: "Dạ vâng anh, em đang chuẩn bị slide và kịch bản test ạ!",
      status: "DELIVERED",
      createdAt: "2026-10-06T14:22:00Z"
    }
  ],
  conv_group_dev: [
    {
      messageId: "msg_grp_01",
      conversationId: "conv_group_dev",
      senderId: "user_nam_002",
      content: "Anh em đã deploy xong bản cập nhật Spring Boot 3.3.5 lên cụm Docker chưa?",
      status: "DELIVERED",
      createdAt: "2026-10-06T10:05:00Z"
    },
    {
      messageId: "msg_grp_02",
      conversationId: "conv_group_dev",
      senderId: "user_mai_003",
      content: "Đã xong nhé! Database PostgreSQL và Cassandra đều đã migration dữ liệu thành công.",
      status: "DELIVERED",
      createdAt: "2026-10-06T10:07:00Z"
    },
    {
      messageId: "msg_grp_03",
      conversationId: "conv_group_dev",
      senderId: "user_dung_004",
      content: "Cổng Nginx API Gateway đang phân phối tải rất đều tới các pods ws-gateway.",
      status: "DELIVERED",
      createdAt: "2026-10-06T10:09:00Z"
    },
    {
      messageId: "msg_grp_04",
      conversationId: "conv_group_dev",
      senderId: "user_admin_001",
      content: "Tuyệt vời! Mọi người kiểm tra lại tính năng AR Meme Matching và WebSocket heartbeat nhé!",
      status: "DELIVERED",
      createdAt: "2026-10-06T10:12:00Z"
    },
    {
      messageId: "msg_grp_05",
      conversationId: "conv_group_dev",
      senderId: "user_an_005",
      content: "Đang test trên browser, độ trễ WebSocket dưới 10ms và âm thanh SFX 3D rất sống động ạ! 🎉",
      status: "DELIVERED",
      createdAt: "2026-10-06T10:15:00Z"
    }
  ]
};

const mockMemes = [
  { id: "m1", emotion: "HAPPY", url: "assets/images/meme-happy-doge.svg" },
  { id: "m2", emotion: "HAPPY", url: "assets/images/meme-happy-success.svg" },
  { id: "m3", emotion: "HAPPY", url: "assets/images/meme-happy-smile.svg" },
  { id: "m4", emotion: "SAD", url: "assets/images/meme-sad-pepe.svg" },
  { id: "m5", emotion: "SURPRISED", url: "assets/images/meme-surprised-pikachu.svg" },
  { id: "m6", emotion: "NEUTRAL", url: "assets/images/meme-neutral-poker.svg" }
];

const mockAuditLogs = [
  JSON.stringify({ timestamp: "2026-10-06T23:15:22Z", ip: "192.168.1.105", path: "/api/auth/login", reason: "Rate Limit Exceeded (120 req/m)", username: "unknown" }),
  JSON.stringify({ timestamp: "2026-10-06T22:50:11Z", ip: "10.0.0.45", path: "/api/auth/login", reason: "Brute Force Password Attack", username: "admin" }),
  JSON.stringify({ timestamp: "2026-10-06T21:34:05Z", ip: "172.16.0.99", path: "/api/messages", reason: "Malformed Packet / XSS Injection", username: "anonymous" }),
  JSON.stringify({ timestamp: "2026-10-06T20:12:44Z", ip: "192.168.1.120", path: "/api/users", reason: "Expired Bearer JWT Token", username: "pham_huong" })
];

const mimeTypes = {
  '.html': 'text/html; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.js': 'application/javascript; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.svg': 'image/svg+xml',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.gif': 'image/gif'
};

const readBody = (req) => new Promise((resolve) => {
  let body = '';
  req.on('data', chunk => { body += chunk; });
  req.on('end', () => {
    try { resolve(JSON.parse(body || '{}')); }
    catch(e) { resolve({}); }
  });
});

const server = http.createServer(async (req, res) => {
  // CORS
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization, X-Requested-With');

  if (req.method === 'OPTIONS') {
    res.writeHead(200);
    res.end();
    return;
  }

  const parsedUrl = new URL(req.url, `http://${req.headers.host}`);
  const pathname = parsedUrl.pathname;

  const sendJson = (data, status = 200) => {
    res.writeHead(status, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify(data));
  };

  // ── AI COPILOT & SMART FEATURES REST APIS (POWERED BY xKiro LLM) ──
  if (pathname === '/api/ai/chat' && req.method === 'POST') {
    const body = await readBody(req);
    const prompt = (body.prompt || '').replace(/@(AI|Copilot|Gemini)/gi, '').trim();
    let reply = null;

    if (prompt) {
      reply = await queryXkiro(
        'Bạn là Gemini AI Copilot - trợ lý AI thông minh trong nền tảng nhắn tin doanh nghiệp Real-time Chat Microservices (Spring Boot, Cassandra, Redis Pub/Sub, WebSocket). Hãy trả lời người dùng bằng tiếng Việt tự nhiên, súc tích, chuyên nghiệp, hỗ trợ giải thích kỹ thuật, viết code và định dạng Markdown đẹp mắt.',
        prompt,
        600
      );
    }

    if (!reply) {
      reply = `### ✨ Phản hồi từ Gemini AI Copilot\n\nTôi đã phân tích câu hỏi của bạn: **"${prompt}"**.\n\nTrong kiến trúc Microservices hiện tại, hệ thống sử dụng kết hợp **Cassandra** cho lưu trữ tin nhắn tốc độ cao và **Redis Pub/Sub** để broadcast dữ liệu đến các Gateway WebSocket với độ trễ dưới 5ms.`;
    }

    return sendJson({
      conversationId: body.conversationId,
      prompt: body.prompt,
      reply,
      model: "xKiro (" + KIRO_MODEL + ")",
      timestamp: new Date().toISOString()
    });
  }

  if (pathname === '/api/ai/summarize' && req.method === 'POST') {
    const body = await readBody(req);
    const convId = body.conversationId;
    const msgs = (mockMessages[convId] || []).filter(m => !m.recalled);
    const textHistory = msgs.slice(-20).map(m => {
      const u = mockUsers.find(x => x.id === m.senderId);
      return `${u ? u.username : 'User'}: ${m.content}`;
    }).join('\n');

    let dynamicSummary = null;
    if (textHistory.length > 20) {
      const prompt = `Dưới đây là đoạn chat gần đây trong phòng chat:\n${textHistory}\n\nHãy tóm tắt nội dung cuộc thảo luận này và trả về JSON theo định dạng sau (chỉ trả về JSON thuần, không bọc markdown \`\`\`json):\n{"topic":"Chủ đề chính ngắn gọn","summary":"Tóm tắt tổng quan 2-3 câu","keyPoints":["Điểm chính 1","Điểm chính 2","Điểm chính 3"],"actionItems":["Việc cần làm 1","Việc cần làm 2"],"sentiment":"Tích cực / Năng suất cao","confidence":99.2}`;
      const aiRaw = await queryXkiro('Bạn là trợ lý AI chuyên tóm tắt cuộc thảo luận và lập biên bản hành động.', prompt, 650);
      if (aiRaw) {
        try {
          const cleanJson = aiRaw.replace(/```json|```/g, '').trim();
          dynamicSummary = JSON.parse(cleanJson);
        } catch(e) {}
      }
    }

    if (!dynamicSummary) {
      dynamicSummary = {
        topic: "Đánh giá tiến độ dự án Microservices Real-time Chat & Kế hoạch Nâng cấp v2.0",
        summary: "Cuộc thảo luận ghi nhận các mốc tiến độ chính: Cụm Cassandra cluster và Redis Pub/Sub hoạt động ổn định, xác thực JWT stateless và giao diện 3D đạt chuẩn. Nhóm đã tích hợp thành công AI Copilot và chuẩn bị kiểm thử tải.",
        keyPoints: [
          "Hạ tầng Docker Compose khởi chạy ổn định 3 microservices chính",
          "Độ trễ truyền tin thời gian thực qua WebSocket duy trì dưới 10ms",
          "Tích hợp thành công trợ lý AI Copilot, Smart Reply và Live Translator"
        ],
        actionItems: [
          "Hoàn tất tài liệu kiểm thử và báo cáo nghiệm thu hệ thống",
          "Tiến hành benchmark tải với 1,000 người dùng đồng thời",
          "Chuẩn bị kịch bản triển khai tự động CI/CD cho môi trường Staging"
        ],
        sentiment: "Rất tích cực & Năng suất cao (99.2%)",
        confidence: 99.4
      };
    }

    return sendJson({
      conversationId: convId,
      messageCount: msgs.length,
      ...dynamicSummary
    });
  }

  if (pathname === '/api/ai/smart-replies' && req.method === 'POST') {
    const body = await readBody(req);
    const last = (body.lastMessage || '').trim();
    let replies = null;

    if (last) {
      const prompt = `Đối phương vừa gửi tin nhắn: "${last}". Hãy gợi ý đúng 3 câu phản hồi ngắn gọn, tự nhiên, lịch sự bằng tiếng Việt theo ngữ cảnh. Trả về đúng định dạng JSON Array: ["câu 1", "câu 2", "câu 3"]. Chỉ trả về JSON array, không kèm bất kỳ từ nào khác.`;
      const aiRaw = await queryXkiro('Bạn là trợ lý gợi ý tin nhắn phản hồi nhanh (Smart Reply).', prompt, 250);
      if (aiRaw) {
        try {
          const cleanJson = aiRaw.replace(/```json|```/g, '').trim();
          const parsed = JSON.parse(cleanJson);
          if (Array.isArray(parsed) && parsed.length > 0) {
            replies = parsed.slice(0, 3);
          }
        } catch(e) {}
      }
    }

    if (!replies || replies.length === 0) {
      const lower = last.toLowerCase();
      replies = ["Tuyệt vời! 👍", "Mình đã nắm được thông tin 💡", "Để mình kiểm tra lại nhé 🔍"];
      if (lower.includes('xong chưa') || lower.includes('thế nào rồi') || lower.includes('tiến độ')) {
        replies = ["Đã hoàn thành và chạy rất mượt! 🚀", "Đang kiểm tra nốt bước cuối bạn nhé ⏳", "Để mình gửi bạn kết quả ngay 📄"];
      } else if (lower.includes('chào') || lower.includes('hello') || lower.includes('hi')) {
        replies = ["Chào bạn! Chúc bạn ngày mới tốt lành ✨", "Hello! Có tin tức gì mới không? 🎯", "Chào nhé, mình có thể giúp gì được bạn? 🤝"];
      }
    }

    return sendJson({ replies });
  }

  if (pathname === '/api/ai/translate' && req.method === 'POST') {
    const body = await readBody(req);
    const text = (body.text || '').trim();
    const target = body.targetLang || 'vi';
    let translated = null;

    if (text) {
      const prompt = `Hãy dịch chuẩn xác đoạn văn bản sau sang tiếng ${target === 'vi' ? 'Việt' : 'Anh'}: "${text}". Chỉ trả về duy nhất bản dịch đã hoàn thiện, không thêm bất kỳ ghi chú hay ngoặc kép nào.`;
      translated = await queryXkiro('Bạn là dịch giả cao cấp hỗ trợ dịch thuật phòng chat thời gian thực.', prompt, 400);
    }

    if (!translated) {
      translated = `[Bản dịch]: ${text}`;
    }

    return sendJson({
      originalText: text,
      sourceLang: 'auto',
      targetLang: target,
      translatedText: translated
    });
  }

  if (pathname === '/api/ai/rewrite' && req.method === 'POST') {
    const body = await readBody(req);
    const text = (body.text || '').trim();
    const tone = (body.tone || 'PROFESSIONAL').toUpperCase();
    let rewritten = null;

    if (text) {
      const toneDesc = tone === 'PROFESSIONAL' ? 'Chuyên nghiệp, chuẩn mực công sở' 
        : tone === 'POLITE' ? 'Lịch sự, khiêm tốn, dạ thưa nhã nhặn' 
        : tone === 'CONCISE' ? 'Ngắn gọn, súc tích, đi thẳng vào trọng tâm' 
        : 'Thân mật, vui vẻ, năng động (Casual)';
      const prompt = `Hãy viết lại câu sau theo phong cách "${toneDesc}": "${text}". Chỉ trả về câu văn đã viết lại, không thêm lời dẫn.`;
      rewritten = await queryXkiro('Bạn là chuyên gia biên tập và tinh chỉnh giọng điệu tin nhắn.', prompt, 350);
    }

    if (!rewritten) {
      rewritten = text;
    }

    return sendJson({ originalText: text, tone, rewrittenText: rewritten });
  }

  if (pathname === '/api/ai/imagine' && req.method === 'POST') {
    const body = await readBody(req);
    const style = (body.style || 'CYBERPUNK').toUpperCase();
    const prompt = body.prompt || 'Cyberpunk Holographic AI Room';
    let imageUrl = "https://images.unsplash.com/photo-1542751371-adc38448a05e?auto=format&fit=crop&w=1200&q=80";
    if (style === '3D' || style === '3D_RENDER') {
      imageUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?auto=format&fit=crop&w=1200&q=80";
    } else if (style === 'ANIME') {
      imageUrl = "https://images.unsplash.com/photo-1578632767115-351597cf2477?auto=format&fit=crop&w=1200&q=80";
    } else if (style === 'PIXEL_ART') {
      imageUrl = "https://images.unsplash.com/photo-1550745165-9bc0b252726f?auto=format&fit=crop&w=1200&q=80";
    }
    return sendJson({
      prompt,
      style,
      imageUrl,
      altText: `Tác phẩm nghệ thuật AI phong cách ${style}: ${prompt}`
    });
  }

  // REST APIs - AUTHENTICATION
  if (pathname === '/api/auth/login' && req.method === 'POST') {
    const body = await readBody(req);
    const identifier = (body.usernameOrEmail || body.username || 'admin').trim();
    let user = mockUsers.find(u => 
      u.username.toLowerCase() === identifier.toLowerCase() || 
      (u.email && u.email.toLowerCase() === identifier.toLowerCase())
    );

    if (!user) {
      const isAdm = identifier.toLowerCase().includes('admin');
      const newUserId = 'user_' + Date.now().toString(36);
      user = {
        id: newUserId,
        username: identifier,
        email: `${identifier.replace(/\s+/g, '_').toLowerCase()}@chatapp.com`,
        role: isAdm ? 'ADMIN' : 'USER',
        banned: false,
        createdAt: new Date().toISOString()
      };
      mockUsers.push(user);
    }

    const token = makeJwt(user.id, user.username, user.role);
    return sendJson({
      accessToken: token,
      userId: user.id,
      username: user.username,
      role: user.role
    });
  }

  if (pathname === '/api/auth/register' && req.method === 'POST') {
    const body = await readBody(req);
    const username = (body.username || 'user_' + Math.floor(Math.random() * 1000)).trim();
    const email = (body.email || `${username}@chatapp.com`).trim();

    let existing = mockUsers.find(u => 
      u.username.toLowerCase() === username.toLowerCase() || 
      (u.email && u.email.toLowerCase() === email.toLowerCase())
    );
    if (existing) {
      const token = makeJwt(existing.id, existing.username, existing.role);
      return sendJson({
        accessToken: token,
        userId: existing.id,
        username: existing.username,
        role: existing.role
      });
    }

    const newUserId = 'user_' + Date.now().toString(36) + '_' + Math.random().toString(36).substring(2, 6);
    const newUser = {
      id: newUserId,
      username: username,
      email: email,
      role: 'USER',
      banned: false,
      createdAt: new Date().toISOString()
    };
    mockUsers.push(newUser);

    // Initialize default AI Copilot conversation for the newly registered user
    const aiConv = {
      id: 'conv_ai_' + newUserId,
      type: 'DIRECT',
      name: '✨ Gemini AI Copilot',
      memberIds: [newUserId, '00000000-0000-0000-0000-0000000000a1'],
      createdAt: new Date().toISOString()
    };
    mockConvs.unshift(aiConv);
    mockMessages[aiConv.id] = [
      {
        messageId: 'msg_ai_' + Date.now(),
        conversationId: aiConv.id,
        senderId: '00000000-0000-0000-0000-0000000000a1',
        content: `Xin chào **${username}**! 👋 Chào mừng bạn gia nhập hệ thống Real-time Chat Microservices. Tôi là **Gemini AI Copilot**, sẵn sàng hỗ trợ bạn 24/7!`,
        status: 'DELIVERED',
        createdAt: new Date().toISOString()
      }
    ];

    const token = makeJwt(newUser.id, newUser.username, newUser.role);
    return sendJson({
      accessToken: token,
      userId: newUser.id,
      username: newUser.username,
      role: newUser.role
    }, 201);
  }

  if (pathname === '/api/users' || pathname === '/api/auth/admin/users') {
    return sendJson(mockUsers);
  }

  if (pathname === '/api/auth/admin/system/stats') {
    return sendJson({
      totalUsers: 1428,
      onlineUsers: 86,
      totalConversations: 342
    });
  }

  if (pathname === '/api/auth/admin/system/health') {
    return sendJson({
      "auth-service": "UP",
      "chat-service": "UP",
      "ws-gateway": "UP"
    });
  }

  if (pathname === '/api/auth/admin/system/maintenance') {
    return sendJson({ maintenance: false });
  }

  if (pathname === '/api/auth/admin/system/blacklist-ip') {
    return sendJson([
      "192.168.1.105",
      "10.0.0.45",
      "172.16.0.99"
    ]);
  }

  if (pathname === '/api/auth/admin/system/logs') {
    return sendJson(mockAuditLogs);
  }

  if (pathname === '/api/auth/admin/stats/tokens') {
    return sendJson({
      admin: 420,
      hoang_nam: 315,
      le_mai: 280,
      tran_dung: 195,
      nguyen_an: 142,
      vu_linh: 88,
      dang_khoa: 65
    });
  }

  if (pathname === '/api/auth/admin/stats/requests') {
    return sendJson({
      "GET /api/conversations": 14250,
      "GET /api/messages": 28400,
      "POST /api/messages": 18920,
      "POST /api/auth/login": 3410,
      "POST /api/files/upload": 1280,
      "GET /api/memes": 4520,
      "POST /api/messages/{id}/reactions": 6730
    });
  }

  if (pathname === '/api/auth/admin/broadcast') {
    return sendJson({ success: true, message: "Broadcast dispatched to Redis Pub/Sub cluster" });
  }

  if (pathname === '/api/auth/admin/stats/user-growth') {
    return sendJson([
      { date: "01/10", count: 1280 },
      { date: "02/10", count: 1315 },
      { date: "03/10", count: 1350 },
      { date: "04/10", count: 1390 },
      { date: "05/10", count: 1410 },
      { date: "06/10", count: 1428 }
    ]);
  }

  // ── CONVERSATIONS APIS (CHAT 1-1, TẠO NHÓM, QUẢN LÝ) ──
  if (pathname === '/api/conversations' && req.method === 'GET') {
    const callerId = getUserIdFromAuth(req);
    let userConvs = mockConvs.filter(c => Array.isArray(c.memberIds) && c.memberIds.includes(callerId));
    if (userConvs.length === 0) {
      // Đảm bảo user mới luôn có cuộc trò chuyện với Gemini AI Copilot
      const aiConv = {
        id: 'conv_ai_' + callerId,
        type: 'DIRECT',
        name: '✨ Gemini AI Copilot',
        memberIds: [callerId, '00000000-0000-0000-0000-0000000000a1'],
        createdAt: new Date().toISOString()
      };
      mockConvs.unshift(aiConv);
      mockMessages[aiConv.id] = [
        {
          messageId: 'msg_ai_init_' + Date.now(),
          conversationId: aiConv.id,
          senderId: '00000000-0000-0000-0000-0000000000a1',
          content: 'Xin chào! 👋 Tôi là **Gemini AI Copilot**. Hãy nhắn tin với tôi hoặc tạo nhóm trò chuyện mới nhé!',
          status: 'DELIVERED',
          createdAt: new Date().toISOString()
        }
      ];
      userConvs = [aiConv];
    }
    return sendJson(userConvs);
  }

  // Kết bạn / Mở cuộc trò chuyện 1-1
  if (pathname === '/api/conversations/direct' && req.method === 'POST') {
    const body = await readBody(req);
    const callerId = getUserIdFromAuth(req);
    const targetUserId = body.targetUserId;

    if (!targetUserId) {
      return sendJson({ message: 'Vui lòng chọn hoặc nhập mã người dùng' }, 400);
    }
    if (callerId === targetUserId) {
      return sendJson({ message: 'Không thể tạo cuộc trò chuyện với chính mình' }, 400);
    }

    // Đảm bảo target user có trong mockUsers
    let targetUser = mockUsers.find(u => u.id === targetUserId);
    if (!targetUser) {
      targetUser = {
        id: targetUserId,
        username: 'User_' + (typeof targetUserId === 'string' ? targetUserId.substring(0, 6) : 'Partner'),
        email: `${targetUserId}@chatapp.com`,
        role: 'USER',
        banned: false,
        createdAt: new Date().toISOString()
      };
      mockUsers.push(targetUser);
    }

    // Nếu đã có cuộc trò chuyện 1-1 trước đó giữa 2 người, trả về luôn
    let existing = mockConvs.find(c => 
      c.type === 'DIRECT' && 
      Array.isArray(c.memberIds) &&
      c.memberIds.includes(callerId) && 
      c.memberIds.includes(targetUserId)
    );

    if (existing) {
      return sendJson(existing, 200);
    }

    const callerUser = mockUsers.find(u => u.id === callerId);
    const convId = 'conv_dir_' + Date.now();
    const newConv = {
      id: convId,
      type: 'DIRECT',
      name: targetUser.username,
      avatarUrl: null,
      memberIds: [callerId, targetUserId],
      createdAt: new Date().toISOString()
    };

    mockConvs.unshift(newConv);
    mockMessages[convId] = [
      {
        messageId: 'msg_' + Date.now(),
        conversationId: convId,
        senderId: callerId,
        content: `👋 Cuộc trò chuyện giữa ${callerUser ? callerUser.username : 'bạn'} và ${targetUser.username} đã được tạo thành công!`,
        status: 'DELIVERED',
        createdAt: new Date().toISOString()
      }
    ];

    if (typeof broadcastWs === 'function') {
      broadcastWs({
        type: 'CONVERSATION_CREATED',
        conversation: newConv
      });
    }

    return sendJson(newConv, 201);
  }

  // Tạo Chat Nhóm
  if (pathname === '/api/conversations/group' && req.method === 'POST') {
    const body = await readBody(req);
    const callerId = getUserIdFromAuth(req);
    const groupName = (body.name || '').trim();
    const avatarUrl = body.avatarUrl || null;
    const requestedMembers = Array.isArray(body.memberUserIds) ? body.memberUserIds : [];
    const memberIds = Array.from(new Set([callerId, ...requestedMembers]));

    if (!groupName) {
      return sendJson({ message: 'Vui lòng nhập tên nhóm chat' }, 400);
    }
    if (memberIds.length < 2) {
      return sendJson({ message: 'Nhóm phải có ít nhất 2 thành viên (bao gồm cả bạn)' }, 400);
    }

    const callerUser = mockUsers.find(u => u.id === callerId);
    const convId = 'conv_grp_' + Date.now();
    const newConv = {
      id: convId,
      type: 'GROUP',
      name: groupName,
      avatarUrl: avatarUrl,
      memberIds: memberIds,
      createdAt: new Date().toISOString()
    };

    mockConvs.unshift(newConv);
    mockMessages[convId] = [
      {
        messageId: 'msg_' + Date.now(),
        conversationId: convId,
        senderId: callerId,
        content: `🎉 ${callerUser ? callerUser.username : 'Thành viên'} đã tạo nhóm "${groupName}" với ${memberIds.length} thành viên!`,
        status: 'DELIVERED',
        createdAt: new Date().toISOString()
      }
    ];

    if (typeof broadcastWs === 'function') {
      broadcastWs({
        type: 'CONVERSATION_CREATED',
        conversation: newConv
      });
    }

    return sendJson(newConv, 201);
  }

  if (pathname.startsWith('/api/conversations/') && req.method === 'DELETE') {
    const id = pathname.replace('/api/conversations/', '');
    const idx = mockConvs.findIndex(c => c.id === id);
    if (idx !== -1) mockConvs.splice(idx, 1);
    delete mockMessages[id];
    return sendJson({ success: true });
  }

  if (pathname === '/api/admin/chat/conversations') {
    return sendJson({
      content: mockConvs.map(c => ({
        id: c.id,
        type: c.type,
        name: c.name,
        memberCount: Array.isArray(c.memberIds) ? c.memberIds.length : 2,
        createdAt: c.createdAt
      }))
    });
  }

  if (pathname === '/api/messages') {
    const convId = parsedUrl.searchParams.get('conversationId') || 'conv_direct_nam';
    return sendJson(mockMessages[convId] || []);
  }

  if (pathname === '/api/memes') {
    return sendJson(mockMemes);
  }

  if (pathname === '/api/files/upload' && req.method === 'POST') {
    // Tiêu thụ luồng tải lên để tránh lỗi Connection Reset trên trình duyệt
    await new Promise((resolve) => {
      req.on('data', () => {});
      req.on('end', resolve);
      req.on('error', resolve);
    });
    const sampleAvatars = [
      "https://images.unsplash.com/photo-1522071820081-009f0129c71c?auto=format&fit=crop&w=400&q=80",
      "https://images.unsplash.com/photo-1517245386807-bb43f82c33c4?auto=format&fit=crop&w=400&q=80",
      "https://images.unsplash.com/photo-1529156069898-49953e39b3ac?auto=format&fit=crop&w=400&q=80"
    ];
    const pickedAvatar = sampleAvatars[Math.floor(Math.random() * sampleAvatars.length)];
    return sendJson({ url: pickedAvatar });
  }

  if (pathname === '/api/gateway/sessions') {
    // Return map: { username: count }
    return sendJson({
      "admin (sess_9f82a1bc)": 2,
      "hoang_nam (sess_e431b2aa)": 1,
      "le_mai (sess_3c9d71fa)": 1,
      "tran_dung (sess_8102bd4a)": 1,
      "nguyen_an (sess_6a7821ef)": 1
    });
  }

  if (pathname === '/api/admin/chat/stats/throughput') {
    return sendJson({
      dataPoints: [
        { minute: 540, count: 45 },
        { minute: 545, count: 72 },
        { minute: 550, count: 110 },
        { minute: 555, count: 165 },
        { minute: 560, count: 210 },
        { minute: 565, count: 285 }
      ]
    });
  }

  if (pathname === '/api/admin/chat/stats/top-users') {
    return sendJson([
      { rank: 1, userId: "admin", messageCount: 3420 },
      { rank: 2, userId: "hoang_nam", messageCount: 2815 },
      { rank: 3, userId: "le_mai", messageCount: 2140 },
      { rank: 4, userId: "tran_dung", messageCount: 1920 },
      { rank: 5, userId: "nguyen_an", messageCount: 1450 },
      { rank: 6, userId: "vu_linh", messageCount: 980 },
      { rank: 7, userId: "dang_khoa", messageCount: 820 }
    ]);
  }

  if (pathname === '/api/admin/chat/stats/storage') {
    return sendJson({ messageCount: 52400, memeCount: 48, cassandraSize: "2.4 GB" });
  }

  // Static File Serving
  let filePath = path.join(CLIENT_DIR, pathname === '/' ? 'index.html' : pathname);
  if (!fs.existsSync(filePath)) {
    filePath = path.join(CLIENT_DIR, 'index.html');
  }

  const ext = path.extname(filePath);
  const contentType = mimeTypes[ext] || 'application/octet-stream';

  fs.readFile(filePath, (err, content) => {
    if (err) {
      res.writeHead(404);
      res.end('Not found');
    } else {
      res.writeHead(200, { 'Content-Type': contentType });
      res.end(content);
    }
  });
});

// WebSocket Server
const wss = new WebSocketServer({ server, path: '/ws' });

function broadcastWs(data) {
  const payload = typeof data === 'string' ? data : JSON.stringify(data);
  wss.clients.forEach(client => {
    if (client.readyState === 1) client.send(payload);
  });
}

function getOnlineUserIds() {
  const ids = new Set(['00000000-0000-0000-0000-0000000000a1']); // AI Bot luôn trực tuyến
  wss.clients.forEach(client => {
    if (client.readyState === 1 && client.userId) {
      ids.add(client.userId);
    }
  });
  ids.add('user_admin_001');
  ids.add('user_nam_002');
  ids.add('user_mai_003');
  return Array.from(ids);
}

wss.on('connection', (ws, req) => {
  // Trích xuất userId từ token truyền trên Query String: /ws?token=...
  let connectedUserId = 'user_admin_001';
  try {
    const wsUrl = new URL(req.url, `http://${req.headers.host || 'localhost'}`);
    const token = wsUrl.searchParams.get('token');
    if (token) {
      const parts = token.split('.');
      if (parts.length >= 2) {
        const payload = JSON.parse(Buffer.from(parts[1], 'base64url').toString('utf8'));
        connectedUserId = payload.userId || payload.sub || connectedUserId;
      }
    }
  } catch (e) {}

  ws.userId = connectedUserId;

  // Handshake
  ws.send(JSON.stringify({
    type: 'CONNECTED',
    userId: ws.userId
  }));

  // Gửi danh sách người dùng đang online
  ws.send(JSON.stringify({
    type: 'ONLINE_USERS',
    userIds: getOnlineUserIds()
  }));

  // Phát tín hiệu người dùng này vừa Online tới các client khác
  broadcastWs({
    type: 'PRESENCE',
    userId: ws.userId,
    status: 'ONLINE'
  });

  ws.on('close', () => {
    const stillConnected = Array.from(wss.clients).some(c => c !== ws && c.readyState === 1 && c.userId === ws.userId);
    if (!stillConnected) {
      broadcastWs({
        type: 'PRESENCE',
        userId: ws.userId,
        status: 'OFFLINE'
      });
    }
  });

  ws.on('message', (msgStr) => {
    try {
      const msg = JSON.parse(msgStr);
      if (msg.type === 'PING') {
        ws.send(JSON.stringify({ type: 'PONG' }));
      } else if (msg.type === 'TYPING') {
        // Forward typing event to all clients
        const typingPayload = JSON.stringify({
          type: 'TYPING',
          conversationId: msg.conversationId,
          userId: ws.userId,
          isTyping: msg.isTyping
        });
        wss.clients.forEach(client => {
          if (client !== ws && client.readyState === 1) {
            client.send(typingPayload);
          }
        });
      } else if (msg.type === 'CHAT') {
        const senderId = ws.userId || 'user_admin_001';
        const newMsg = {
          messageId: 'msg_' + Date.now(),
          conversationId: msg.conversationId,
          senderId: senderId,
          content: msg.content,
          mediaUrls: msg.mediaUrls || [],
          replyToId: msg.replyToId || null,
          status: 'DELIVERED',
          createdAt: new Date().toISOString()
        };

        if (!mockMessages[msg.conversationId]) {
          mockMessages[msg.conversationId] = [];
        }
        mockMessages[msg.conversationId].push(newMsg);

        // Broadcast user message
        const broadcastPayload = JSON.stringify({
          type: 'MESSAGE',
          ...newMsg
        });
        broadcastWs(broadcastPayload);

        // Tự động kiểm tra trigger AI Copilot
        const isAiConv = msg.conversationId.startsWith('conv_ai_') || msg.conversationId === 'conv_ai_copilot';
        const isAiTag = /@(AI|Copilot|Gemini)/i.test(msg.content);

        if (isAiConv || isAiTag) {
          // Gửi typing indicator từ AI Bot
          const typingPayload = JSON.stringify({
            type: 'TYPING',
            conversationId: msg.conversationId,
            userId: '00000000-0000-0000-0000-0000000000a1',
            isTyping: true
          });
          broadcastWs(typingPayload);

          const promptClean = (msg.content || '').replace(/@(AI|Copilot|Gemini)/gi, '').trim();

          // Gọi xKiro LLM bất đồng bộ
          (async () => {
            let botReply = await queryXkiro(
              'Bạn là Gemini AI Copilot - trợ lý AI thông minh trong nền tảng nhắn tin doanh nghiệp Real-time Chat Microservices. Hãy trả lời câu hỏi bằng tiếng Việt tự nhiên, súc tích, chuyên sâu, hỗ trợ giải thích kỹ thuật và định dạng Markdown đẹp.',
              promptClean,
              650
            );

            if (!botReply) {
              botReply = `Xin chào! 👋 Tôi là **Gemini AI Copilot**.\n\nTôi đã nhận được câu hỏi: **"${promptClean}"**.\n\nHệ thống đang vận hành các công nghệ Microservices (Spring Boot 3.3, Cassandra, Redis Pub/Sub, WebSocket). Hãy hỏi tôi bất kỳ điều gì nhé!`;
            }

            // Tắt typing
            broadcastWs({
              type: 'TYPING',
              conversationId: msg.conversationId,
              userId: '00000000-0000-0000-0000-0000000000a1',
              isTyping: false
            });

            const aiMsg = {
              messageId: 'msg_ai_' + Date.now(),
              conversationId: msg.conversationId,
              senderId: '00000000-0000-0000-0000-0000000000a1',
              content: botReply,
              mediaUrls: [],
              replyToId: newMsg.messageId,
              replyToContent: newMsg.content,
              replyToSenderId: newMsg.senderId,
              status: 'DELIVERED',
              createdAt: new Date().toISOString()
            };

            mockMessages[msg.conversationId].push(aiMsg);
            broadcastWs({
              type: 'MESSAGE',
              ...aiMsg
            });
          })().catch(() => {
            broadcastWs({
              type: 'TYPING',
              conversationId: msg.conversationId,
              userId: '00000000-0000-0000-0000-0000000000a1',
              isTyping: false
            });
          });
        }
      }
    } catch (e) {}
  });
});

server.listen(PORT, () => {
  console.log(`🚀 Mock Server & WebSocket running at http://localhost:${PORT}`);
  console.log(`✨ AI Copilot endpoints ready at http://localhost:${PORT}/api/ai/*`);
  console.log(`👥 Group & Direct chat endpoints active at http://localhost:${PORT}/api/conversations/*`);
});
