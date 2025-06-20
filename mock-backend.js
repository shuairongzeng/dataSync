const http = require('http');
const url = require('url');

// CORS headers
const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Methods': 'GET, POST, PUT, DELETE, OPTIONS',
  'Access-Control-Allow-Headers': 'Content-Type, Authorization',
  'Access-Control-Max-Age': '3600'
};

// Mock data
const mockData = {
  chartData: [
    {
      name: "需求人数",
      value: 36000,
      percent: "+88%",
      color: "#41b6ff",
      bgColor: "#effaff",
      duration: 2200,
      data: [2101, 5288, 4239, 4962, 6752, 5208, 7450]
    },
    {
      name: "提问数量",
      value: 16580,
      percent: "+70%",
      color: "#e85f33",
      bgColor: "#fff5f4",
      duration: 1600,
      data: [2216, 1148, 1255, 788, 4821, 1973, 4379]
    },
    {
      name: "解决数量",
      value: 16499,
      percent: "+99%",
      color: "#26ce83",
      bgColor: "#eff8f4",
      duration: 1500,
      data: [861, 1002, 3195, 1715, 3666, 2415, 3645]
    },
    {
      name: "用户满意度",
      value: 100,
      percent: "+100%",
      color: "#7846e5",
      bgColor: "#f6f4fe",
      duration: 100,
      data: [100]
    }
  ],
  
  barChartData: [
    {
      requireData: [2101, 5288, 4239, 4962, 6752, 5208, 7450],
      questionData: [2216, 1148, 1255, 1788, 4821, 1973, 4379]
    },
    {
      requireData: [2101, 3280, 4400, 4962, 5752, 6889, 7600],
      questionData: [2116, 3148, 3255, 3788, 4821, 4970, 5390]
    }
  ],
  
  progressData: [
    { week: "周日", percentage: 100, duration: 80, color: "#26ce83" },
    { week: "周六", percentage: 96, duration: 85, color: "#26ce83" },
    { week: "周五", percentage: 94, duration: 90, color: "#26ce83" },
    { week: "周四", percentage: 89, duration: 95, color: "#41b6ff" },
    { week: "周三", percentage: 88, duration: 100, color: "#41b6ff" },
    { week: "周二", percentage: 86, duration: 105, color: "#41b6ff" },
    { week: "周一", percentage: 85, duration: 110, color: "#41b6ff" }
  ]
};

const server = http.createServer((req, res) => {
  const parsedUrl = url.parse(req.url, true);
  const path = parsedUrl.pathname;
  const method = req.method;

  // Set CORS headers
  Object.keys(corsHeaders).forEach(key => {
    res.setHeader(key, corsHeaders[key]);
  });

  // Handle preflight requests
  if (method === 'OPTIONS') {
    res.writeHead(200);
    res.end();
    return;
  }

  // Set content type
  res.setHeader('Content-Type', 'application/json');

  try {
    // Health check
    if (path === '/api/test/health') {
      res.writeHead(200);
      res.end(JSON.stringify({
        status: "UP",
        service: "DbSync Mock Backend",
        timestamp: Date.now()
      }));
      return;
    }

    // Dashboard APIs
    if (path === '/api/dashboard/chart-data') {
      res.writeHead(200);
      res.end(JSON.stringify(mockData.chartData));
      return;
    }

    if (path === '/api/dashboard/bar-chart-data') {
      res.writeHead(200);
      res.end(JSON.stringify(mockData.barChartData));
      return;
    }

    if (path === '/api/dashboard/progress-data') {
      res.writeHead(200);
      res.end(JSON.stringify(mockData.progressData));
      return;
    }

    // Auth API
    if (path === '/api/auth/signin' && method === 'POST') {
      let body = '';
      req.on('data', chunk => {
        body += chunk.toString();
      });
      req.on('end', () => {
        try {
          const loginData = JSON.parse(body);
          if (loginData.username === 'admin' && loginData.password === 'admin123') {
            res.writeHead(200);
            res.end(JSON.stringify({
              token: "mock-jwt-token-12345",
              username: "admin",
              email: "admin@dbsync.com",
              role: "ADMIN"
            }));
          } else {
            res.writeHead(400);
            res.end(JSON.stringify({ error: "用户名或密码错误" }));
          }
        } catch (e) {
          res.writeHead(400);
          res.end(JSON.stringify({ error: "Invalid JSON" }));
        }
      });
      return;
    }

    // 404 for other routes
    res.writeHead(404);
    res.end(JSON.stringify({ error: "Not Found" }));

  } catch (error) {
    res.writeHead(500);
    res.end(JSON.stringify({ error: "Internal Server Error" }));
  }
});

const PORT = 8080;
server.listen(PORT, () => {
  console.log(`Mock backend server running on http://localhost:${PORT}`);
  console.log('Available endpoints:');
  console.log('  GET  /api/test/health');
  console.log('  GET  /api/dashboard/chart-data');
  console.log('  GET  /api/dashboard/bar-chart-data');
  console.log('  GET  /api/dashboard/progress-data');
  console.log('  POST /api/auth/signin');
});
