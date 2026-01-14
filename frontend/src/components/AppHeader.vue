<template>
  <header class="app-header">
    <div class="brand" @click="goHome">
      <span class="brand-mark">BOJ</span>
    </div>

    <nav class="nav-links">
      <RouterLink to="/questions" class="nav-link">题库</RouterLink>
      <RouterLink to="/posts" class="nav-link">讨论</RouterLink>
      <RouterLink to="/submissions" class="nav-link">提交记录</RouterLink>
      <RouterLink to="/me" class="nav-link">个人中心</RouterLink>
      <RouterLink v-if="auth.isAdmin" to="/admin/questions" class="nav-link">管理题目</RouterLink>
      <RouterLink v-if="auth.isAdmin" to="/admin/users" class="nav-link">用户管理</RouterLink>
    </nav>

    <div class="user-actions header-login">
      <template v-if="!auth.user">
        <RouterLink class="btn btn-primary login-button" to="/login">登录</RouterLink>
      </template>
      <div v-else class="user-chip">
        <div class="user-pill">
          <img v-if="auth.user.userAvatar" :src="auth.user.userAvatar" class="avatar avatar-sm" alt="avatar" />
          <div class="user-meta">
            <div class="user-name">{{ displayName }}</div>
            <div class="user-role">{{ roleLabel }}</div>
          </div>
        </div>
        <button class="btn btn-ghost logout-button" type="button" @click="logout">退出</button>
      </div>
    </div>
  </header>
</template>

<script setup>
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '../stores/auth';

const auth = useAuthStore();
const router = useRouter();

const displayName = computed(() => {
  if (!auth.user) {
    return '';
  }
  return auth.user.userName || `用户${auth.user.id}`;
});

const roleLabel = computed(() => {
  if (!auth.user) {
    return '';
  }
  if (auth.user.userRole === 'admin') {
    return '管理员';
  }
  if (auth.user.userRole === 'ban') {
    return '封禁';
  }
  return '用户';
});

function goHome() {
  router.push('/questions');
}

async function logout() {
  await auth.logout();
  router.push('/questions');
}
</script>