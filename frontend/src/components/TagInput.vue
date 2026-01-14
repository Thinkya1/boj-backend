<template>
  <div class="tag-input">
    <div class="tag-list">
      <span v-for="tag in modelValue" :key="tag" class="tag">
        {{ tag }}
        <button type="button" class="tag-remove" @click="removeTag(tag)">×</button>
      </span>
    </div>
    <input
      v-model="input"
      class="input"
      :placeholder="placeholder"
      @keydown="handleKeydown"
      @blur="commit"
    />
  </div>
</template>

<script setup>
import { ref } from 'vue';

const props = defineProps({
  modelValue: {
    type: Array,
    default: () => [],
  },
  placeholder: {
    type: String,
    default: '输入标签后回车',
  },
});

const emit = defineEmits(['update:modelValue']);
const input = ref('');

function handleKeydown(event) {
  if (event.key === 'Enter' || event.key === ',') {
    event.preventDefault();
    commit();
  }
}

function commit() {
  const value = input.value.trim();
  if (!value) {
    input.value = '';
    return;
  }
  const next = Array.from(new Set([...props.modelValue, value]));
  emit('update:modelValue', next);
  input.value = '';
}

function removeTag(tag) {
  emit('update:modelValue', props.modelValue.filter((item) => item !== tag));
}
</script>
