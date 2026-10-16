import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'

import {
  Alert,
  Avatar,
  Button,
  Card,
  Dropdown,
  Empty,
  Form,
  Input,
  InputNumber,
  Layout,
  Menu,
  Modal,
  Pagination,
  Popconfirm,
  Radio,
  Select,
  Space,
  Spin,
  Statistic,
  Table,
  Tag,
  Tooltip,
} from 'ant-design-vue'
import 'ant-design-vue/dist/reset.css'
import '@/assets/theme.css'
import '@/access'

const antDesignComponents = [
  Alert,
  Avatar,
  Button,
  Card,
  Dropdown,
  Empty,
  Form,
  Input,
  InputNumber,
  Layout,
  Menu,
  Modal,
  Pagination,
  Popconfirm,
  Radio,
  Select,
  Space,
  Spin,
  Statistic,
  Table,
  Tag,
  Tooltip,
]

const app = createApp(App)

app.use(createPinia())
app.use(router)
antDesignComponents.forEach((component) => app.use(component))
app.mount('#app')
