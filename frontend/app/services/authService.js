// Basic structure for authService.js
// We'll need to know the actual API endpoint and how the JWT is returned and stored.

// Assume this is your API base URL, configure as needed
const API_URL = '/api/auth/'; // Example, adjust to your Spring Boot backend

class AuthService {
  async login(username, password) {
    // This is a placeholder.
    // In a real app, you'd make an HTTP POST request to your backend.
    // e.g., using fetch or axios
    // const response = await fetch(API_URL + 'login', {
    //   method: 'POST',
    //   headers: { 'Content-Type': 'application/json' },
    //   body: JSON.stringify({ username, password }),
    // });

    // if (!response.ok) {
    //   const errorData = await response.json().catch(() => ({ message: 'Login failed' }));
    //   throw new Error(errorData.message || 'Login failed');
    // }

    // const data = await response.json();
    // if (data.token) {
    //   localStorage.setItem('jwt_token', data.token);
    //   // Optionally store user info if returned
    //   // localStorage.setItem('user', JSON.stringify(data.user));
    //   return data; // Contains token and potentially user info
    // } else {
    //   throw new Error('Token not found in response');
    // }

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
    localStorage.removeItem('user'); // If user info is stored
    console.log('Logged out, token removed.');
    // Optionally, notify backend about logout
  }

  getToken() {
    return localStorage.getItem('jwt_token');
  }

  isAuthenticated() {
    const token = this.getToken();
    // Add more robust token validation if needed (e.g., check expiration)
    return !!token;
  }

  // getUser() {
  //   const user = localStorage.getItem('user');
  //   return user ? JSON.parse(user) : null;
  // }
}

export default new AuthService();
