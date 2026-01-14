<template>
  <div class="grid" style="place-items: center;">
    <div class="card" style="max-width: 420px; width: 100%;">
      <h2 class="section-title">欢迎回来</h2>
      <p class="muted" style="margin-top: 6px;">登录后继续刷题与交流。</p>

      <form class="stack" style="margin-top: 20px;" @submit.prevent="submit">
        <label class="stack" style="gap: 6px;">
          <span>账号</span>
          <input v-model="form.userAccount" class="input" placeholder="输入账号" />
        </label>
        <label class="stack" style="gap: 6px;">
          <span>密码</span>
          <input v-model="form.userPassword" type="password" class="input" placeholder="输入密码" />
        </label>
        <button class="btn btn-primary" :disabled="loading" type="submit">
          {{ loading ? '登录中...' : '登录' }}
        </button>
      </form>

      <div class="hint" style="margin-top: 14px;">
        没有账号？<RouterLink to="/register" class="nav-link">去注册</RouterLink>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '../stores/auth';
import { useToastStore } from '../stores/toast';

const form = reactive({
  userAccount: '',
  userPassword: '',
});
const loading = ref(false);
const auth = useAuthStore();
const toast = useToastStore();
const route = useRoute();
const router = useRouter();

async function submit() {
  if (!form.userAccount || !form.userPassword) {
    toast.push('请输入账号与密码', 'error');
    return;
  }
  loading.value = true;
  try {
    await auth.login(form);
    toast.push('登录成功', 'info');
    const redirect = route.query.redirect ? String(route.query.redirect) : '/questions';
    router.push(redirect);
  } finally {
    loading.value = false;
  }
}
</script>
