import { defineStore } from 'pinia';

let seed = 1;

export const useToastStore = defineStore('toast', {
  state: () => ({
    items: [],
  }),
  actions: {
    push(message, type = 'info', timeout = 2600) {
      const id = seed++;
      this.items.push({ id, message, type });
      setTimeout(() => this.remove(id), timeout);
    },
    remove(id) {
      this.items = this.items.filter((item) => item.id !== id);
    },
  },
});
