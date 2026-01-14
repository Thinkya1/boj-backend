<template>
  <div class="app-shell" :class="{ 'full-bleed': isFullBleed }">
    <AppHeader />
    <main class="app-main" :class="{ 'full-bleed': isFullBleed }">
      <RouterView />
    </main>
    <AppFooter />
    <ToastStack />
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import AppHeader from './components/AppHeader.vue';
import AppFooter from './components/AppFooter.vue';
import ToastStack from './components/ToastStack.vue';
import { useAuthStore } from './stores/auth';

const route = useRoute();
const auth = useAuthStore();
const isFullBleed = computed(() => route.meta.fullBleed);

onMounted(() => {
  auth.bootstrap();
});
</script>
