export function formatDate(value) {
  if (!value) {
    return '-';
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return '-';
  }
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date);
}

const LANGUAGE_LABELS = {
  java: 'Java',
  gcc: 'C',
  javascript: 'JavaScript',
  python: 'Python',
};

export function formatLanguage(value) {
  if (!value) {
    return '-';
  }
  return LANGUAGE_LABELS[value] || value;
}

export function formatStatus(status) {
  switch (status) {
    case 0:
      return { text: '等待判题', tone: 'muted' };
    case 1:
      return { text: '判题中', tone: 'info' };
    case 2:
      return { text: '成功', tone: 'success' };
    case 3:
      return { text: '失败', tone: 'danger' };
    default:
      return { text: '未知', tone: 'muted' };
  }
}

const RESULT_LABELS = {
  AC: '通过',
  WA: '答案错误',
  CE: '编译错误',
  RE: '运行错误',
  TLE: '超时',
  MLE: '内存超限',
  PE: '展示错误',
  OLE: '输出超限',
  SE: '系统错误',
  DANGEROUS: '危险操作',
  WAITING: '等待判题',
};

const MESSAGE_LABELS = {
  Accepted: '通过',
  'Wrong Answer': '答案错误',
  'Compile Error': '编译错误',
  'Runtime Error': '运行错误',
  'Time Limit Exceeded': '超时',
  'Memory Limit Exceeded': '内存超限',
  'Presentation Error': '展示错误',
  'Output Limit Exceeded': '输出超限',
  'Dangerous Operation': '危险操作',
  'System Error': '系统错误',
  Waiting: '等待判题',
};

function normalizeJudgeMessage(message) {
  if (!message) {
    return '';
  }
  const trimmed = message.trim();
  return MESSAGE_LABELS[trimmed] || trimmed;
}

function resolveResultLabel(result) {
  if (!result) {
    return null;
  }
  const label = RESULT_LABELS[result];
  if (!label) {
    return null;
  }
  const tone = result === 'AC' ? 'success' : result === 'WAITING' ? 'muted' : 'danger';
  return { text: label, tone };
}

function isAccepted(message) {
  if (!message) {
    return false;
  }
  const normalized = message.toLowerCase();
  return (
    normalized.includes('accepted') ||
    normalized.includes('ac') ||
    normalized.includes('成功') ||
    normalized.includes('通过')
  );
}

function getJudgeTone(message) {
  if (!message) {
    return 'muted';
  }
  if (isAccepted(message)) {
    return 'success';
  }
  return 'danger';
}

export function formatResult(status, judgeMessage, result) {
  if (status === 0) {
    return { text: '等待判题', tone: 'muted' };
  }
  if (status === 1) {
    return { text: '判题中', tone: 'info' };
  }
  const resultLabel = resolveResultLabel(result);
  if (resultLabel) {
    return resultLabel;
  }
  const message = normalizeJudgeMessage(judgeMessage);
  if (message) {
    return { text: message, tone: getJudgeTone(message) };
  }
  if (status === 2) {
    return { text: '成功', tone: 'success' };
  }
  if (status === 3) {
    return { text: '失败', tone: 'danger' };
  }
  return { text: '未知', tone: 'muted' };
}

export function formatJudgeMessage(message) {
  const normalized = normalizeJudgeMessage(message);
  return normalized || '-';
}

export function formatMemory(value) {
  if (value === null || value === undefined) {
    return '-';
  }
  return `${value} KB`;
}

export function formatTime(value) {
  if (value === null || value === undefined) {
    return '-';
  }
  return `${value} ms`;
}

export function parseTags(input) {
  if (!input) {
    return [];
  }
  return input
    .split(/[,，]/g)
    .map((item) => item.trim())
    .filter((item) => item.length > 0);
}

export function formatTags(tags) {
  if (!tags || !tags.length) {
    return '';
  }
  return tags.join(', ');
}
