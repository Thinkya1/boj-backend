<template>
  <div class="pagination" v-if="totalPages > 1">
    <button class="btn btn-ghost" :disabled="page <= 1" @click="go(page - 1)">上一页</button>
    <button
      v-for="item in pages"
      :key="item"
      class="btn"
      :class="item === page ? 'btn-primary' : 'btn-ghost'"
      @click="go(item)"
    >
      {{ item }}
    </button>
    <button class="btn btn-ghost" :disabled="page >= totalPages" @click="go(page + 1)">下一页</button>
  </div>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  page: {
    type: Number,
    default: 1,
  },
  pageSize: {
    type: Number,
    default: 10,
  },
  total: {
    type: Number,
    default: 0,
  },
});

const emit = defineEmits(['update:page']);

const totalPages = computed(() => Math.max(1, Math.ceil(props.total / props.pageSize)));

const pages = computed(() => {
  const windowSize = 5;
  const half = Math.floor(windowSize / 2);
  let start = Math.max(1, props.page - half);
  let end = Math.min(totalPages.value, start + windowSize - 1);
  if (end - start < windowSize - 1) {
    start = Math.max(1, end - windowSize + 1);
  }
  const result = [];
  for (let i = start; i <= end; i += 1) {
    result.push(i);
  }
  return result;
});

function go(target) {
  if (target < 1 || target > totalPages.value || target === props.page) {
    return;
  }
  emit('update:page', target);
}
</script>
