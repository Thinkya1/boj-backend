<template>
  <div class="grid" style="place-items: center;">
    <div class="card" style="max-width: 440px; width: 100%;">
      <h2 class="section-title">创建账号</h2>
      <p class="muted" style="margin-top: 6px;">加入 BOJ，开始高效训练。</p>

      <form class="stack" style="margin-top: 20px;" @submit.prevent="submit">
        <label class="stack" style="gap: 6px;">
          <span>账号</span>
          <input v-model="form.userAccount" class="input" placeholder="至少 4 位" />
        </label>
        <label class="stack" style="gap: 6px;">
          <span>密码</span>
          <input v-model="form.userPassword" type="password" class="input" placeholder="至少 8 位" />
        </label>
        <label class="stack" style="gap: 6px;">
          <span>确认密码</span>
          <input v-model="form.checkPassword" type="password" class="input" placeholder="再次输入密码" />
        </label>
        <button class="btn btn-primary" :disabled="loading" type="submit">
          {{ loading ? '注册中...' : '注册' }}
        </button>
      </form>

      <div class="hint" style="margin-top: 14px;">
        已有账号？<RouterLink to="/login" class="nav-link">去登录</RouterLink>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '../stores/auth';
import { useToastStore } from '../stores/toast';

const form = reactive({
  userAccount: '',
  userPassword: '',
  checkPassword: '',
});
const loading = ref(false);
const auth = useAuthStore();
const toast = useToastStore();
const router = useRouter();

async function submit() {
  if (!form.userAccount || !form.userPassword || !form.checkPassword) {
    toast.push('请完整填写注册信息', 'error');
    return;
  }
  if (form.userPassword !== form.checkPassword) {
    toast.push('两次密码不一致', 'error');
    return;
  }
  loading.value = true;
  try {
    await auth.register(form);
    toast.push('注册成功，请登录', 'info');
    router.push('/login');
  } finally {
    loading.value = false;
  }
}
</script>
