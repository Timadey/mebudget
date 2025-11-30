import { supabase } from '../lib/supabase';
import bcrypt from 'bcryptjs';

export const settingsService = {
    // Get all settings for the current user
    getSettings: async () => {
        try {
            const { data, error } = await supabase
                .from('settings')
                .select('*')
                .single();

            if (error) {
                // If row doesn't exist (e.g. old user before trigger), try to create it or return defaults
                if (error.code === 'PGRST116') {
                    return await settingsService.initializeSettings();
                }
                throw error;
            }
            return data;
        } catch (error) {
            console.error('Error getting settings:', error);
            return null;
        }
    },

    // Initialize settings if missing (fallback)
    initializeSettings: async () => {
        const { data: { user } } = await supabase.auth.getUser();
        if (!user) return null;

        const { data, error } = await supabase
            .from('settings')
            .insert({ user_id: user.id })
            .select()
            .single();

        if (error) {
            console.error('Error initializing settings:', error);
            return null;
        }
        return data;
    },

    // Update a specific column
    updateSetting: async (column, value) => {
        try {
            const { data: { user } } = await supabase.auth.getUser();
            if (!user) throw new Error('No user logged in');

            const { error } = await supabase
                .from('settings')
                .update({ [column]: value })
                .eq('user_id', user.id);

            if (error) throw error;
            return true;
        } catch (error) {
            console.error(`Error updating ₦{column}:`, error);
            return false;
        }
    },

    // PIN-specific methods
    isPinEnabled: async () => {
        const settings = await settingsService.getSettings();
        return settings?.pin_enabled || false;
    },

    setPinEnabled: async (enabled) => {
        return await settingsService.updateSetting('pin_enabled', enabled);
    },

    getPinHash: async () => {
        const settings = await settingsService.getSettings();
        return settings?.pin_hash;
    },

    setPinHash: async (pin) => {
        const hash = await bcrypt.hash(pin, 10);
        return await settingsService.updateSetting('pin_hash', hash);
    },

    async verifyPin(pin) {
        const hash = await settingsService.getPinHash();
        if (!hash) return true;
        return await bcrypt.compare(pin, hash);
    },

    async getOnboardingCompleted() {
        const settings = await settingsService.getSettings();
        return settings?.onboarding_completed || false;
    },

    async setOnboardingCompleted(completed) {
        return await settingsService.updateSetting('onboarding_completed', completed);
    },

    updateLastVerified: async () => {
        const now = new Date().toISOString();
        return await settingsService.updateSetting('last_verified_at', now);
    },

    getLastVerified: async () => {
        const settings = await settingsService.getSettings();
        return settings?.last_verified_at ? new Date(settings.last_verified_at) : null;
    },

    needsVerification: async () => {
        const settings = await settingsService.getSettings();
        if (!settings?.pin_enabled) return false;

        const lastVerified = settings.last_verified_at ? new Date(settings.last_verified_at) : null;
        if (!lastVerified) return true;

        const twoHoursAgo = new Date(Date.now() - 2 * 60 * 60 * 1000);
        return lastVerified < twoHoursAgo;
    },

    // Budget duration methods
    getBudgetDuration: async () => {
        const settings = await settingsService.getSettings();
        return settings?.budget_duration || 'monthly';
    },

    setBudgetDuration: async (duration) => {
        return await settingsService.updateSetting('budget_duration', duration);
    }
};
