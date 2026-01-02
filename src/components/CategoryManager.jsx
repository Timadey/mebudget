import React, { useState } from 'react';
import { api } from '../services/api';
import { Trash2, Edit2, Plus, Save, X, Loader2 } from 'lucide-react';
import clsx from 'clsx';

export default function CategoryManager({ categories, onUpdate, currentPeriod }) {
    const [editingId, setEditingId] = useState(null);
    const [isCreating, setIsCreating] = useState(false);
    const [loading, setLoading] = useState(false);
    const [applyToPeriod, setApplyToPeriod] = useState(true);

    // Form state
    const [formData, setFormData] = useState({
        name: '',
        type: 'Expense',
        budgetLimit: 0,
        icon: '🏷️'
    });

    const resetForm = () => {
        setFormData({ name: '', type: 'Expense', budgetLimit: 0, icon: '🏷️' });
        setEditingId(null);
        setIsCreating(false);
        setApplyToPeriod(true);
    };

    const handleEdit = (category) => {
        setFormData({
            name: category.name,
            type: category.type,
            budgetLimit: category.budgetlimit || 0, // This is the effective limit for the context
            icon: category.icon || '🏷️'
        });
        setEditingId(category.id);
        setIsCreating(false);
        // Default to applying to period if we have one, but maybe check if it differs from default?
        // Simplicity: default to true.
        setApplyToPeriod(!!currentPeriod);
    };

    const handleDelete = async (id) => {
        if (!window.confirm('Are you sure? This will not delete associated transactions but might affect reports.')) return;
        setLoading(true);
        try {
            await api.deleteCategory(id);
            onUpdate();
        } catch (error) {
            alert('Failed to delete category');
        } finally {
            setLoading(false);
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);
        try {
            let catId = editingId;

            // 1. Create or Update Category (Global Properties)
            const globalUpdates = {
                name: formData.name,
                type: formData.type,
                icon: formData.icon,
                // If NOT applying to period (i.e., setting default), we update budget_limit here
                budgetLimit: !applyToPeriod ? formData.budgetLimit : undefined
            };

            const cleanUpdates = Object.fromEntries(
                Object.entries(globalUpdates).filter(([_, v]) => v !== undefined)
            );

            if (isCreating) {
                // When creating, we always set the default limit first
                const { data } = await api.createCategory({
                    ...formData,
                    budgetLimit: formData.budgetLimit // Initial default
                });
                if (data && data[0]) catId = data[0].id;
            } else if (editingId) {
                // If applying to period, we DON'T update the default limit in the category definition
                // If NOT applying to period, we DO update it.
                // But wait, we always need to update name/icon etc.
                if (!applyToPeriod) {
                    await api.updateCategory(editingId, {
                        ...formData,
                        budgetLimit: formData.budgetLimit
                    });
                } else {
                    // Only update name/icon/type
                    await api.updateCategory(editingId, {
                        name: formData.name,
                        type: formData.type,
                        icon: formData.icon
                    });
                }
            }

            // 2. Set Period Budget if applicable
            if (applyToPeriod && currentPeriod && catId && formData.type === 'Expense') {
                await api.setBudget(
                    catId,
                    formData.budgetLimit,
                    currentPeriod.start,
                    currentPeriod.end
                );
            }

            onUpdate();
            resetForm();
        } catch (error) {
            console.error(error);
            alert('Failed to save category');
        } finally {
            setLoading(false);
        }
    };

    const formatDate = (date) => {
        return new Date(date).toLocaleDateString(undefined, { month: 'short', year: 'numeric' });
    };

    const periodLabel = currentPeriod ? `${formatDate(currentPeriod.start)} - ${formatDate(currentPeriod.end)}` : 'Period';

    return (
        <div className="space-y-4">
            {/* List Header */}
            <div className="flex justify-between items-center">
                <p className="text-slate-400 text-sm">Manage your budget categories</p>
                {!isCreating && !editingId && (
                    <button
                        onClick={() => setIsCreating(true)}
                        className="text-sm bg-primary/20 text-primary hover:bg-primary/30 px-3 py-1.5 rounded-lg transition-colors flex items-center gap-2"
                    >
                        <Plus size={16} /> New Category
                    </button>
                )}
            </div>

            {/* Create/Edit Form */}
            {(isCreating || editingId) && (
                <form onSubmit={handleSubmit} className="bg-white/5 p-4 rounded-xl border border-white/10 space-y-3">
                    <div className="grid grid-cols-2 gap-3">
                        <div>
                            <label className="text-xs text-slate-400 block mb-1">Name</label>
                            <input
                                type="text"
                                required
                                value={formData.name}
                                onChange={e => setFormData({ ...formData, name: e.target.value })}
                                className="w-full bg-slate-900/50 border border-white/10 rounded-lg px-3 py-1.5 text-sm text-white focus:ring-1 focus:ring-primary outline-none"
                            />
                        </div>
                        <div>
                            <label className="text-xs text-slate-400 block mb-1">Type</label>
                            <select
                                value={formData.type}
                                onChange={e => setFormData({ ...formData, type: e.target.value })}
                                className="w-full bg-slate-900/50 border border-white/10 rounded-lg px-3 py-1.5 text-sm text-white focus:ring-1 focus:ring-primary outline-none"
                            >
                                <option value="Expense">Expense</option>
                                <option value="Income">Income</option>
                                <option value="Investment">Investment</option>
                            </select>
                        </div>
                        <div>
                            <label className="text-xs text-slate-400 block mb-1">Budget Limit (₦)</label>
                            <input
                                type="number"
                                value={formData.budgetLimit}
                                onChange={e => setFormData({ ...formData, budgetLimit: e.target.value })}
                                className="w-full bg-slate-900/50 border border-white/10 rounded-lg px-3 py-1.5 text-sm text-white focus:ring-1 focus:ring-primary outline-none"
                            />
                        </div>
                        <div>
                            <label className="text-xs text-slate-400 block mb-1">Icon</label>
                            <input
                                type="text"
                                value={formData.icon}
                                onChange={e => setFormData({ ...formData, icon: e.target.value })}
                                className="w-full bg-slate-900/50 border border-white/10 rounded-lg px-3 py-1.5 text-sm text-white focus:ring-1 focus:ring-primary outline-none"
                                placeholder="e.g. 🏠"
                            />
                        </div>
                    </div>

                    {/* Period Toggle */}
                    {currentPeriod && !isCreating && formData.type === 'Expense' && (
                        <div className="flex items-center gap-2 pt-1">
                            <input
                                type="checkbox"
                                id="applyToPeriod"
                                checked={applyToPeriod}
                                onChange={(e) => setApplyToPeriod(e.target.checked)}
                                className="rounded bg-slate-900 border-white/10 text-primary focus:ring-primary"
                            />
                            <label htmlFor="applyToPeriod" className="text-xs text-slate-300">
                                Apply only to {periodLabel}
                            </label>
                        </div>
                    )}

                    <div className="flex justify-end gap-2 pt-2">
                        <button
                            type="button"
                            onClick={resetForm}
                            className="px-3 py-1.5 text-sm text-slate-400 hover:text-white transition-colors"
                        >
                            Cancel
                        </button>
                        <button
                            type="submit"
                            disabled={loading}
                            className="bg-primary hover:bg-primary-dark text-white px-3 py-1.5 rounded-lg text-sm flex items-center gap-2 transition-colors"
                        >
                            {loading ? <Loader2 size={14} className="animate-spin" /> : <Save size={14} />}
                            Save
                        </button>
                    </div>
                </form>
            )}

            {/* Categories List */}
            <div className="space-y-2 max-h-[400px] overflow-y-auto pr-1">
                {categories.map((cat) => (
                    <div key={cat.id} className="flex items-center justify-between p-3 bg-white/5 rounded-lg border border-white/5 group hover:border-white/10 transition-colors">
                        <div className="flex items-center gap-3">
                            <div className="w-8 h-8 rounded-lg bg-slate-800 flex items-center justify-center text-lg">
                                {cat.icon || '🏷️'}
                            </div>
                            <div>
                                <p className="font-medium text-white">{cat.name}</p>
                                <p className="text-xs text-slate-400">
                                    {cat.type} • Limit: ₦{Number(cat.budgetlimit || 0).toLocaleString()}
                                </p>
                            </div>
                        </div>
                        <div className="flex items-center gap-1 opacity-100 md:opacity-0 group-hover:opacity-100 transition-opacity">
                            <button
                                onClick={() => handleEdit(cat)}
                                className="p-2 text-slate-400 hover:text-primary hover:bg-primary/10 rounded-lg transition-colors"
                            >
                                <Edit2 size={16} />
                            </button>
                            <button
                                onClick={() => handleDelete(cat.id)}
                                className="p-2 text-slate-400 hover:text-rose-400 hover:bg-rose-500/10 rounded-lg transition-colors"
                            >
                                <Trash2 size={16} />
                            </button>
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );
}
