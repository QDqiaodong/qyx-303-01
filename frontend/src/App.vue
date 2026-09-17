<template>
  <div class="app-container">
    <el-header class="header">
      <h1>企业团建方案多方案条件对比择优推演系统</h1>
    </el-header>

    <el-container>
      <el-aside class="aside" width="200px">
        <el-menu :default-active="activeTab" class="aside-menu" @select="handleTabChange">
          <el-menu-item index="plan">
            <el-icon><Document /></el-icon>
            <span>团建方案管理</span>
          </el-menu-item>
          <el-menu-item index="constraint">
            <el-icon><Setting /></el-icon>
            <span>约束条件管理</span>
          </el-menu-item>
          <el-menu-item index="compare">
            <el-icon><DataAnalysis /></el-icon>
            <span>方案对比择优</span>
          </el-menu-item>
          <el-menu-item index="batch">
            <el-icon><Tickets /></el-icon>
            <span>落地批次与预算池</span>
          </el-menu-item>
        </el-menu>
      </el-aside>

      <el-main class="main">
        <PlanManagement v-if="activeTab === 'plan'" @refresh="loadData" />
        <ConstraintManagement v-if="activeTab === 'constraint'" @refresh="loadData" />
        <PlanCompare v-if="activeTab === 'compare'" />
        <BatchManagement v-if="activeTab === 'batch'" />
      </el-main>
    </el-container>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Document, Setting, DataAnalysis, Tickets } from '@element-plus/icons-vue'
import PlanManagement from './components/PlanManagement.vue'
import ConstraintManagement from './components/ConstraintManagement.vue'
import PlanCompare from './components/PlanCompare.vue'
import BatchManagement from './components/BatchManagement.vue'

const activeTab = ref('plan')

const handleTabChange = (tab) => {
  activeTab.value = tab
}

const loadData = () => {
}

onMounted(() => {
})
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', Arial, sans-serif;
}

.app-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  text-align: center;
  padding: 20px 0;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.header h1 {
  font-size: 24px;
  font-weight: 600;
}

.aside {
  background: #2d3748;
}

.aside-menu {
  height: 100%;
  border-right: none;
}

.aside-menu :deep(.el-menu-item) {
  color: #a0aec0;
}

.aside-menu :deep(.el-menu-item:hover) {
  color: white;
  background: #4a5568;
}

.aside-menu :deep(.el-menu-item.is-active) {
  color: white;
  background: #667eea;
}

.main {
  padding: 20px;
  background: #f7fafc;
}
</style>