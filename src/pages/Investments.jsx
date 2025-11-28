import React, { useState, useEffect } from 'react';
import { TrendingUp, PieChart, ArrowUpRight, Settings } from 'lucide-react';
import { api } from '../services/api';
import Modal from '../components/Modal';
import InvestmentManager from '../components/InvestmentManager';
import clsx from 'clsx';

export default function Investments() {
    const [data, setData] = useState({ investments: [] });
    const [loading, setLoading] = useState(true);
    const [showManager, setShowManager] = useState(false);

    const fetchData = async () => {
        setLoading(true);
        try {
            const result = await api.getData();
            setData(result);
        } catch (error) {
            console.error('Failed to fetch data', error);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchData();
    }, []);

    const totalValue = data.investments.reduce((sum, inv) => sum + Number(inv.currentbalance), 0);
    const totalTarget = data.investments.reduce((sum, inv) => sum + Number(inv.targetamount), 0);
    const progress = totalTarget > 0 ? (totalValue / totalTarget) * 100 : 0;

    return (
        <div className="space-y-6">
            <header className="flex items-center justify-between">
                <div>
                    <h2 className="text-3xl font-bold text-white">Investments</h2>
                    <p className="text-slate-400">Track your portfolio and savings goals.</p>
                </div>
                <button
                    onClick={() => setShowManager(true)}
                    className="bg-primary hover:bg-primary-dark text-white px-4 py-2 rounded-lg flex items-center gap-2 transition-colors font-medium"
                >
                    <Settings size={20} />
                    Manage Investments
                </button>
            </header>

            {/* Investment Manager Modal */}
            <Modal
                isOpen={showManager}
                onClose={() => setShowManager(false)}
                title="Manage Investments"
            >
                <InvestmentManager
                    investments={data.investments}
                    onUpdate={fetchData}
                />
            </Modal>

            {/* Portfolio Overview */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                <div className="glass-card md:col-span-2">
                    <div className="flex items-center gap-3 mb-4">
                        <div className="p-2 bg-emerald-500/20 text-emerald-400 rounded-lg">
                            <TrendingUp size={24} />
                        </div>
                        <div>
                            <p className="text-slate-400 text-sm">Total Portfolio Value</p>
                            <h3 className="text-3xl font-bold text-white">₦{totalValue.toLocaleString()}</h3>
                        </div>
                    </div>
                    <div className="w-full bg-slate-700 h-2 rounded-full overflow-hidden mb-2">
                        <div className="bg-emerald-500 h-full rounded-full" style={{ width: `${Math.min(progress, 100)}%` }}></div>
                    </div>
                    <p className="text-sm text-slate-400">
                        {progress.toFixed(1)}% of your ₦{totalTarget.toLocaleString()} goal
                    </p>
                </div>

                <div className="glass-card flex flex-col justify-center items-center text-center">
                    <div className="p-3 bg-blue-500/20 text-blue-400 rounded-full mb-3">
                        <PieChart size={32} />
                    </div>
                    <h4 className="text-xl font-bold text-white">{data.investments.length}</h4>
                    <p className="text-slate-400 text-sm">Active Assets</p>
                </div>
            </div>

            {/* Investment List */}
            <h3 className="text-xl font-bold text-white mt-8 mb-4">Your Assets</h3>
            {loading ? (
                <div className="text-center text-slate-400 py-10">Loading investments...</div>
            ) : (
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                    {data.investments.map((inv) => {
                        const current = Number(inv.currentbalance);
                        const target = Number(inv.targetamount);
                        const p = target > 0 ? (current / target) * 100 : 0;

                        return (
                            <div key={inv.id} className="glass-card hover:bg-white/10 transition-colors">
                                <div className="flex justify-between items-start mb-4">
                                    <div className="p-2 bg-slate-700 rounded-lg">
                                        <ArrowUpRight size={20} className="text-emerald-400" />
                                    </div>
                                    <span className="text-xs font-medium px-2 py-1 rounded-full bg-slate-700 text-slate-300">
                                        {p.toFixed(0)}%
                                    </span>
                                </div>

                                <h4 className="text-lg font-bold text-white mb-1">{inv.category}</h4>
                                <p className="text-slate-400 text-sm mb-4">Target: ₦{target.toLocaleString()}</p>

                                <div className="flex justify-between items-end">
                                    <div>
                                        <p className="text-xs text-slate-500 mb-1">Current Balance</p>
                                        <p className="text-xl font-bold text-white">₦{current.toLocaleString()}</p>
                                    </div>
                                </div>
                            </div>
                        );
                    })}
                </div>
            )}
        </div>
    );
}
