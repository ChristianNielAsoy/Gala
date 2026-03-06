<template>
  <div class="empty-state text-center q-pa-xl">
    <q-icon :name="icon" size="64px" color="grey-4" class="q-mb-md" />
    <div class="text-h6 text-grey-6">{{ title }}</div>
    <div v-if="subtitle" class="text-body2 text-grey-5 q-mt-xs">{{ subtitle }}</div>
    <q-btn
      v-if="actionLabel"
      flat
      no-caps
      color="primary"
      :label="actionLabel"
      class="q-mt-md"
      @click="$emit('action')"
    />
  </div>
</template>

<script setup lang="ts">
defineProps<{
  icon: string;
  title: string;
  subtitle?: string;
  actionLabel?: string;
}>();

defineEmits<{ action: [] }>();
</script>

<style scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
}
</style>
