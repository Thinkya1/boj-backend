<template>
  <div class="stack">
    <div class="card">
      <h2 class="section-title">{{ isEdit ? '编辑题目' : '创建题目' }}</h2>
      <p class="muted">按结构化方式补全题面与判题配置。</p>
      <p v-if="isEdit" class="hint" style="margin-top: 8px;">
        编辑无法读取题目答案/用例，如需保留请重新填写，否则会覆盖为空。
      </p>
    </div>

    <form class="stack" @submit.prevent="submit">
      <div class="card stack">
        <label class="stack" style="gap: 6px;">
          <span>标题</span>
          <input v-model="form.title" class="input" placeholder="题目名称" />
        </label>
        <label class="stack" style="gap: 6px;">
          <span>标签</span>
          <TagInput v-model="form.tags" />
        </label>
        <label class="stack" style="gap: 6px;">
          <span>题目描述</span>
          <textarea v-model="form.content" class="textarea" placeholder="题目描述、输入输出说明等"></textarea>
        </label>
        <label class="stack" style="gap: 6px;">
          <span>标准答案</span>
          <textarea v-model="form.answer" class="textarea" placeholder="参考答案"></textarea>
        </label>
      </div>

      <div class="card stack">
        <h3>判题配置</h3>
        <div class="grid grid-3">
          <label class="stack" style="gap: 6px;">
            <span>时间限制 (ms)</span>
            <input v-model.number="form.judgeConfig.timeLimit" type="number" class="input" />
          </label>
          <label class="stack" style="gap: 6px;">
            <span>内存限制 (KB)</span>
            <input v-model.number="form.judgeConfig.memoryLimit" type="number" class="input" />
          </label>
          <label class="stack" style="gap: 6px;">
            <span>栈限制 (KB)</span>
            <input v-model.number="form.judgeConfig.stackLimit" type="number" class="input" />
          </label>
        </div>
      </div>

      <div class="card stack">
        <h3>判题用例</h3>
        <div v-if="!form.judgeCase.length" class="muted">暂无用例，点击“添加用例”。</div>
        <div v-for="(item, index) in form.judgeCase" :key="index" class="card" style="box-shadow: none;">
          <div class="card-header">
            <div>用例 {{ index + 1 }}</div>
            <button type="button" class="btn btn-ghost" @click="removeCase(index)">删除</button>
          </div>
          <div class="grid grid-2">
            <label class="stack" style="gap: 6px;">
              <span>输入</span>
              <textarea v-model="item.input" class="textarea"></textarea>
            </label>
            <label class="stack" style="gap: 6px;">
              <span>输出</span>
              <textarea v-model="item.output" class="textarea"></textarea>
            </label>
          </div>
        </div>
        <button type="button" class="btn btn-ghost" @click="addCase">添加用例</button>
      </div>

      <div style="display: flex; gap: 12px;">
        <button class="btn btn-primary" type="submit" :disabled="loading">{{ loading ? '保存中...' : '保存' }}</button>
        <button class="btn btn-ghost" type="button" @click="cancel">取消</button>
      </div>
    </form>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { get, post } from '../api/http';
import { useToastStore } from '../stores/toast';
import TagInput from '../components/TagInput.vue';

const route = useRoute();
const router = useRouter();
const toast = useToastStore();
const loading = ref(false);

const form = reactive({
  id: null,
  title: '',
  content: '',
  tags: [],
  answer: '',
  judgeConfig: {
    timeLimit: 1000,
    memoryLimit: 262144,
    stackLimit: 262144,
  },
  judgeCase: [],
});

const isEdit = computed(() => Boolean(route.params.id));

function addCase() {
  form.judgeCase.push({ input: '', output: '' });
}

function removeCase(index) {
  form.judgeCase.splice(index, 1);
}

async function loadQuestion() {
  if (!isEdit.value) {
    return;
  }
  const data = await get('/question/get', { params: { id: route.params.id } });
  form.id = data.id;
  form.title = data.title || '';
  form.content = data.content || '';
  form.tags = parseJson(data.tags, []);
  form.answer = data.answer || '';
  form.judgeCase = parseJson(data.judgeCase, []);
  form.judgeConfig = parseJson(data.judgeConfig, form.judgeConfig);
}

async function submit() {
  if (!form.title || !form.content) {
    toast.push('请填写题目标题与内容', 'error');
    return;
  }
  loading.value = true;
  try {
    const payload = {
      id: form.id,
      title: form.title,
      content: form.content,
      tags: form.tags,
      answer: form.answer,
      judgeCase: form.judgeCase,
      judgeConfig: form.judgeConfig,
    };
    if (isEdit.value) {
      await post('/question/edit', payload);
    } else {
      await post('/question/add', payload);
    }
    toast.push('保存成功', 'info');
    router.push('/questions');
  } finally {
    loading.value = false;
  }
}

function cancel() {
  router.back();
}

function parseJson(value, fallback) {
  if (!value) {
    return fallback;
  }
  try {
    return JSON.parse(value);
  } catch (error) {
    return fallback;
  }
}

onMounted(loadQuestion);
</script>
