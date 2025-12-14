// Activity logging utilities
// src/utils/activityLogger.ts

import { supabase } from 'boot/supabase';
import type { ActivityActionType, ActivityEntityType } from 'src/types/expense';

export interface ActivityLogData {
  trip_id: string;
  user_id?: string | undefined;
  action_type: ActivityActionType;
  entity_type?: ActivityEntityType;
  entity_id?: string;
  description: string;
  metadata?: Record<string, unknown>;
}

/**
 * Log an activity to the activity_log table
 */
export async function logActivity(activity: ActivityLogData): Promise<void> {
  try {
    const { error } = await supabase.from('activity_log').insert({
      trip_id: activity.trip_id,
      user_id: activity.user_id,
      action_type: activity.action_type,
      entity_type: activity.entity_type,
      entity_id: activity.entity_id,
      description: activity.description,
      metadata: activity.metadata || {},
    });

    if (error) {
      console.error('Failed to log activity:', error);
      // Don't throw error to avoid breaking the main flow
    }
  } catch (error) {
    console.error('Error logging activity:', error);
  }
}

/**
 * Get current user ID for activity logging
 */
export async function getCurrentUserId(): Promise<string | undefined> {
  try {
    const { data } = await supabase.auth.getUser();
    return data.user?.id;
  } catch (error) {
    console.error('Error getting current user:', error);
    return undefined;
  }
}
