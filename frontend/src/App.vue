<template>
  <div id="app">
    <NavHeader />
    <main class="main-content">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import NavHeader from '@/components/NavHeader.vue'
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}
body {
  background: #f5f7fa;
}
#app {
  min-height: 100vh;
}
.main-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}
</style>