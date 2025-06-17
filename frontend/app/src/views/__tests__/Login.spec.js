import { describe, it, expect, vi } from 'vitest';
import { mount } from '@vue/test-utils';
import Login from '@/views/Login.vue';
import authService from '@/services/authService';

// Mock Vue Router
const mockRouter = {
  push: vi.fn(),
};

vi.mock('vue-router', () => ({
  useRouter: () => mockRouter,
  // If Login.vue uses <router-link>, it might need to be stubbed or mocked too
  // For example, provide a dummy RouterLink component:
  RouterLink: { template: '<a><slot /></a>' }
}));

// Mock authService
vi.mock('@/services/authService', () => ({
  default: {
    login: vi.fn(),
  },
}));

describe('Login.vue', () => {
  it('renders login form correctly', () => {
    const wrapper = mount(Login, {
      global: {
        stubs: {
          RouterLink: true // Stubbing vue-router's RouterLink if it's directly used
        }
      }
    });
    expect(wrapper.find('h2').text()).toBe('Login');
    expect(wrapper.find('label[for="username"]').exists()).toBe(true);
    expect(wrapper.find('input#username').exists()).toBe(true);
    expect(wrapper.find('label[for="password"]').exists()).toBe(true);
    expect(wrapper.find('input#password').exists()).toBe(true);
    expect(wrapper.find('button[type="submit"]').exists()).toBe(true);
  });

  it('calls authService.login on form submission and redirects on success', async () => {
    authService.login.mockResolvedValue({ token: 'fake-token' });
    mockRouter.push.mockClear(); // Clear previous calls if any

    const wrapper = mount(Login, {
      global: {
        stubs: { RouterLink: true }
      }
    });

    await wrapper.find('input#username').setValue('testuser');
    await wrapper.find('input#password').setValue('password');
    await wrapper.find('form').trigger('submit.prevent');

    expect(authService.login).toHaveBeenCalledWith('testuser', 'password');
    // Ensure async operations complete for router push
    await wrapper.vm.$nextTick(); // Wait for Vue to process updates
    await new Promise(resolve => setTimeout(resolve, 0)); // Wait for microtasks like router navigation

    expect(mockRouter.push).toHaveBeenCalledWith('/dashboard');
  });

  it('displays error message on failed login', async () => {
    const errorMessage = 'Invalid credentials';
    authService.login.mockRejectedValue(new Error(errorMessage));
    mockRouter.push.mockClear(); // Clear previous calls

    const wrapper = mount(Login, {
      global: {
        stubs: { RouterLink: true }
      }
    });

    await wrapper.find('input#username').setValue('wronguser');
    await wrapper.find('input#password').setValue('wrongpassword');
    await wrapper.find('form').trigger('submit.prevent');

    await wrapper.vm.$nextTick(); // Ensure DOM updates for error message

    const errorParagraph = wrapper.find('p.error-message');
    expect(errorParagraph.exists()).toBe(true);
    expect(errorParagraph.text()).toBe(errorMessage);
    expect(mockRouter.push).not.toHaveBeenCalled(); // Ensure no redirect
  });
});
