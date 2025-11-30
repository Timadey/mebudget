import { supabase } from '../lib/supabase';
import bcrypt from 'bcryptjs';

export const settingsService = {
    // Get a setting by key
    getSetting: async (key) => {
        try {
            const { data, error } = await supabase
                .from('settings')
                .select('*')
                .eq('key', key)
                .single();

            if (error) throw error;

            // Parse value based on type
            if (!data) return null;

            switch (data.value_type) {
                case 'boolean':
                    return data.value === 'true';
                case 'number':
                    return Number(data.value);
                case 'json':
                    return JSON.parse(data.value);
                default:
                    return data.value;
            }
        } catch (error) {
            console.error('Error getting setting:', error);
            return null;
        }
    },

    // Set a setting
    setSetting: async (key, value, valueType = 'string') => {
        try {
            const stringValue = valueType === 'json' ? JSON.stringify(value) : String(value);

            const { data: { user } } = await supabase.auth.getUser();
            if (!user) throw new Error('No user logged in');

            const { error } = await supabase
                .from('settings')
                .upsert({
                    user_id: user.id,
                    key,
                    value: stringValue,
                    value_type: valueType
                }, { onConflict: 'user_id, key' });

            if (error) throw error;
            return true;
        } catch (error) {
            console.error('Error setting setting:', error);
            return false;
        }
    },

    // PIN-specific methods
    isPinEnabled: async () => {
        return await settingsService.getSetting('pin_enabled');
    },

    setPinEnabled: async (enabled) => {
        return await settingsService.setSetting('pin_enabled', enabled, 'boolean');
    },

    getPinHash: async () => {
        return await settingsService.getSetting('pin_hash');
    },

    setPinHash: async (pin) => {
        const hash = await bcrypt.hash(pin, 10);
        return await settingsService.setSetting('pin_hash', hash, 'string');
    },

    verifyPin: async (pin) => {
        const hash = await settingsService.getPinHash();
        if (!hash) return false;
        return await bcrypt.compare(pin, hash);
    },

    updateLastVerified: async () => {
        const now = new Date().toISOString();
        return await settingsService.setSetting('last_verified_at', now, 'string');
    },

    getLastVerified: async () => {
        const timestamp = await settingsService.getSetting('last_verified_at');
        return timestamp ? new Date(timestamp) : null;
    },

    needsVerification: async () => {
        const enabled = await settingsService.isPinEnabled();
        if (!enabled) return false;

        const lastVerified = await settingsService.getLastVerified();
        if (!lastVerified) return true;

        const twoHoursAgo = new Date(Date.now() - 2 * 60 * 60 * 1000);
        return lastVerified < twoHoursAgo;
    },

    // Budget duration methods
    getBudgetDuration: async () => {
        const duration = await settingsService.getSetting('budget_duration');
        return duration || 'monthly';
    },

    setBudgetDuration: async (duration) => {
        return await settingsService.setSetting('budget_duration', duration, 'string');
    }
};
