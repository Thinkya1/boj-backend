import { defineStore } from 'pinia';
import { get, post } from '../api/http';

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: null,
    ready: false,
  }),
  getters: {
    isAdmin: (state) => state.user?.userRole === 'admin',
  },
  actions: {
    async bootstrap() {
      if (this.ready) {
        return;
      }
      try {
        await this.fetchCurrentUser();
      } finally {
        this.ready = true;
      }
    },
    async fetchCurrentUser() {
      try {
        const data = await get('/user/get/login', { silent: true });
        this.user = data || null;
        return data;
      } catch (error) {
        this.user = null;
        return null;
      }
    },
    async login(payload) {
      const data = await post('/user/login', payload);
      this.user = data || null;
      return data;
    },
    async register(payload) {
      return post('/user/register', payload);
    },
    async logout() {
      await post('/user/logout');
      this.user = null;
    },
    async updateProfile(payload) {
      await post('/user/update/my', payload);
      await this.fetchCurrentUser();
    },
  },
});
