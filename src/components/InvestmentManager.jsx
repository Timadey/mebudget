import React, { useState } from 'react';
import { api } from '../services/api';
import { Trash2, Edit2, Plus, Save, Loader2 } from 'lucide-react';

export default function InvestmentManager({ investments, onUpdate }) {
    const [editingId, setEditingId] = useState(null);
    const [isCreating, setIsCreating] = useState(false);
    const [loading, setLoading] = useState(false);

    // Form state
    const [formData, setFormData] = useState({
        category: '',
        targetAmount: 0,
        currentBalance: 0
    });

    const resetForm = () => {
        setFormData({ category: '', targetAmount: 0, currentBalance: 0 });
        setEditingId(null);
        setIsCreating(false);
    };

    const handleEdit = (investment) => {
        setFormData({
            category: investment.category,
            targetAmount: investment.targetamount || 0,
            currentBalance: investment.currentbalance || 0
        });
        setEditingId(investment.id);
        setIsCreating(false);
    };

    const handleDelete = async (id) => {
        if (!window.confirm('Are you sure you want to delete this investment?')) return;
        setLoading(true);
        try {
            await api.deleteInvestment(id);
            onUpdate();
        } catch (error) {
            alert('Failed to delete investment');
        } finally {
            setLoading(false);
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);
        try {
            if (isCreating) {
                await api.createInvestment(formData);
            } else if (editingId) {
                await api.updateInvestment(editingId, formData);
            }
            onUpdate();
            resetForm();
        } catch (error) {
            alert('Failed to save investment');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="space-y-4">
            {/* List Header */}
            <div className="flex justify-between items-center">
                <p className="text-slate-400 text-sm">Manage your investment portfolio</p>
                {!isCreating && !editingId && (
                    <button
                        onClick={() => setIsCreating(true)}
                        className="text-sm bg-primary/20 text-primary hover:bg-primary/30 px-3 py-1.5 rounded-lg transition-colors flex items-center gap-2"
                    >
                        <Plus size={16} /> New Investment
                    </button>
                )}
            </div>

            {/* Create/Edit Form */}
            {(isCreating || editingId) && (
                <form onSubmit={handleSubmit} className="bg-white/5 p-4 rounded-xl border border-white/10 space-y-3">
                    <div className="grid grid-cols-1 gap-3">
                        <div>
                            <label className="text-xs text-slate-400 block mb-1">Name</label>
                            <input
                                type="text"
                                required
                                value={formData.category}
                                onChange={e => setFormData({ ...formData, category: e.target.value })}
                                className="w-full bg-slate-900/50 border border-white/10 rounded-lg px-3 py-1.5 text-sm text-white focus:ring-1 focus:ring-primary outline-none"
                                placeholder="e.g. Tesla Stock, Gold"
                            />
                        </div>
                        <div className="grid grid-cols-2 gap-3">
                            <div>
                                <label className="text-xs text-slate-400 block mb-1">Target Amount</label>
                                <input
                                    type="number"
                                    value={formData.targetAmount}
                                    onChange={e => setFormData({ ...formData, targetAmount: e.target.value })}
                                    className="w-full bg-slate-900/50 border border-white/10 rounded-lg px-3 py-1.5 text-sm text-white focus:ring-1 focus:ring-primary outline-none"
                                />
                            </div>
                            <div>
                                <label className="text-xs text-slate-400 block mb-1">Current Balance</label>
                                <input
                                    type="number"
                                    value={formData.currentBalance}
                                    onChange={e => setFormData({ ...formData, currentBalance: e.target.value })}
                                    className="w-full bg-slate-900/50 border border-white/10 rounded-lg px-3 py-1.5 text-sm text-white focus:ring-1 focus:ring-primary outline-none"
                                />
                            </div>
                        </div>
                    </div>
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

            {/* Investments List */}
            <div className="space-y-2 max-h-[400px] overflow-y-auto pr-1">
                {investments.map((inv) => (
                    <div key={inv.id} className="flex items-center justify-between p-3 bg-white/5 rounded-lg border border-white/5 group hover:border-white/10 transition-colors">
                        <div>
                            <p className="font-medium text-white">{inv.category}</p>
                            <p className="text-xs text-slate-400">
                                ₦{Number(inv.currentbalance).toLocaleString()} / ₦{Number(inv.targetamount).toLocaleString()}
                            </p>
                        </div>
                        <div className="flex items-center gap-1 opacity-100 md:opacity-0 group-hover:opacity-100 transition-opacity">
                            <button
                                onClick={() => handleEdit(inv)}
                                className="p-2 text-slate-400 hover:text-primary hover:bg-primary/10 rounded-lg transition-colors"
                            >
                                <Edit2 size={16} />
                            </button>
                            <button
                                onClick={() => handleDelete(inv.id)}
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
