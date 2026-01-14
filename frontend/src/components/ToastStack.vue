<template>
  <div class="toast-stack">
    <div v-for="item in toast.items" :key="item.id" class="toast" :class="`toast-${item.type}`">
      {{ item.message }}
    </div>
  </div>
</template>

<script setup>
import { useToastStore } from '../stores/toast';

const toast = useToastStore();
</script>
