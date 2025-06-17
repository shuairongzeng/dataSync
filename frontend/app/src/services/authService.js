const API_URL = '/api/auth/';

class AuthService {
  async login(username, password) {
    // MOCK IMPLEMENTATION FOR NOW:
    return new Promise((resolve, reject) => {
      setTimeout(() => {
        if (username === 'admin' && password === 'password') {
          const mockToken = 'mock_jwt_token_12345';
          localStorage.setItem('jwt_token', mockToken);
          console.log('Mock login successful, token stored.');
          resolve({ token: mockToken, user: { username: 'admin' } });
        } else {
          console.log('Mock login failed.');
          reject(new Error('Invalid mock credentials'));
        }
      }, 500);
    });
  }

  logout() {
    localStorage.removeItem('jwt_token');
    localStorage.removeItem('user');
    console.log('Logged out, token removed.');
  }

  getToken() {
    return localStorage.getItem('jwt_token');
  }

  isAuthenticated() {
    const token = this.getToken();
    return !!token;
  }
}

export default new AuthService();
