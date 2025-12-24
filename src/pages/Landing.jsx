import React from 'react';
import { Link } from 'react-router-dom';
import { ArrowRight, Wallet, TrendingDown, Shield, CheckCircle2, AlertTriangle, TrendingUp, BarChart3, PieChart } from 'lucide-react';
import clsx from 'clsx';

export default function Landing() {
    return (
        <div className="min-h-screen bg-dark-900 text-white overflow-x-hidden">
            {/* Navigation */}
            <nav className="fixed top-0 left-0 right-0 z-50 bg-dark-900/80 backdrop-blur-md border-b border-white/5">
                <div className="max-w-7xl mx-auto px-6 h-20 flex items-center justify-between">
                    <div className="flex items-center gap-2">
                        <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-primary to-secondary flex items-center justify-center text-white shadow-lg shadow-primary/20">
                            <Wallet size={24} />
                        </div>
                        <span className="text-xl font-bold bg-clip-text text-transparent bg-gradient-to-r from-white to-slate-400">
                            MeBudget
                        </span>
                    </div>
                    <div className="flex items-center gap-4">
                        <Link to="/login" className="text-slate-400 hover:text-white font-medium transition-colors">
                            Login
                        </Link>
                        <Link
                            to="/register"
                            className="bg-primary hover:bg-primary-dark text-white px-5 py-2.5 rounded-xl font-bold transition-all hover:scale-105 active:scale-95 shadow-lg shadow-primary/20"
                        >
                            Get Started
                        </Link>
                    </div>
                </div>
            </nav>

            {/* Hero Section */}
            <section className="pt-32 pb-20 px-6 relative">
                {/* Background Gradients */}
                <div className="absolute top-20 left-1/2 -translate-x-1/2 w-[800px] h-[500px] bg-primary/20 rounded-full blur-[120px] -z-10" />
                <div className="absolute bottom-0 right-0 w-[600px] h-[400px] bg-secondary/10 rounded-full blur-[100px] -z-10" />

                <div className="max-w-5xl mx-auto text-center mb-16">
                    <div className="inline-flex items-center gap-2 px-4 py-2 rounded-full bg-white/5 border border-white/10 mb-8 animate-in fade-in slide-in-from-bottom-4 duration-700">
                        <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
                        <span className="text-sm text-slate-300">Simple, Secure, Smart</span>
                    </div>
                    <h1 className="text-5xl md:text-7xl font-bold mb-6 leading-tight animate-in fade-in slide-in-from-bottom-8 duration-700 delay-100">
                        Master Your Money <br />
                        <span className="bg-clip-text text-transparent bg-gradient-to-r from-primary via-secondary to-primary bg-300% animate-gradient">
                            Without the Stress
                        </span>
                    </h1>
                    <p className="text-xl text-slate-400 mb-10 max-w-2xl mx-auto animate-in fade-in slide-in-from-bottom-8 duration-700 delay-200">
                        Track expenses, set budgets, visualize spending patterns, and achieve your financial goals with powerful analytics and a beautifully simple interface.
                    </p>
                    <div className="flex flex-col sm:flex-row items-center justify-center gap-4 animate-in fade-in slide-in-from-bottom-8 duration-700 delay-300">
                        <Link
                            to="/register"
                            className="w-full sm:w-auto bg-white text-dark-900 px-8 py-4 rounded-xl font-bold text-lg flex items-center justify-center gap-2 hover:bg-slate-200 transition-all hover:scale-105 active:scale-95"
                        >
                            Start for Free <ArrowRight size={20} />
                        </Link>
                        <Link
                            to="/login"
                            className="w-full sm:w-auto px-8 py-4 rounded-xl font-bold text-lg flex items-center justify-center gap-2 border border-white/10 hover:bg-white/5 transition-all"
                        >
                            Sign In
                        </Link>
                    </div>
                </div>

                {/* Mock Interface with Analytics */}
                <div className="max-w-6xl mx-auto relative animate-in fade-in slide-in-from-bottom-12 duration-1000 delay-500">
                    <div className="absolute -inset-1 bg-gradient-to-r from-primary to-secondary rounded-2xl opacity-20 blur-lg" />
                    <div className="relative bg-dark-800/80 backdrop-blur-xl border border-white/10 rounded-2xl p-4 md:p-8 shadow-2xl overflow-hidden">
                        {/* Mock Header */}
                        <div className="flex items-center justify-between mb-8 border-b border-white/5 pb-4">
                            <div className="flex items-center gap-4">
                                <div className="w-10 h-10 rounded-full bg-slate-700" />
                                <div>
                                    <div className="h-4 w-32 bg-slate-700 rounded mb-2" />
                                    <div className="h-3 w-20 bg-slate-800 rounded" />
                                </div>
                            </div>
                            <div className="flex gap-2">
                                <div className="w-10 h-10 rounded-lg bg-slate-700" />
                                <div className="w-10 h-10 rounded-lg bg-slate-700" />
                            </div>
                        </div>

                        {/* Mock Grid */}
                        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-6">
                            {/* Card 1: Total Balance */}
                            <div className="bg-gradient-to-br from-primary/20 to-primary/5 border border-primary/20 rounded-xl p-6">
                                <div className="flex items-center gap-3 mb-4">
                                    <div className="p-2 bg-primary/20 rounded-lg text-primary">
                                        <Wallet size={20} />
                                    </div>
                                    <span className="text-slate-300">Total Balance</span>
                                </div>
                                <div className="text-3xl font-bold text-white mb-2">₦2,450,000</div>
                                <div className="flex items-center gap-2 text-emerald-400 text-sm">
                                    <TrendingUp size={16} />
                                    <span>+12% this month</span>
                                </div>
                            </div>

                            {/* Card 2: Monthly Spending */}
                            <div className="bg-white/5 border border-white/10 rounded-xl p-6">
                                <div className="flex items-center gap-3 mb-4">
                                    <div className="p-2 bg-rose-500/20 rounded-lg text-rose-400">
                                        <TrendingDown size={20} />
                                    </div>
                                    <span className="text-slate-300">Spending</span>
                                </div>
                                <div className="text-3xl font-bold text-white mb-2">₦185,000</div>
                                <div className="w-full bg-slate-700 h-2 rounded-full overflow-hidden">
                                    <div className="bg-rose-500 h-full w-[65%]" />
                                </div>
                                <div className="mt-2 text-xs text-slate-400 text-right">65% of budget</div>
                            </div>

                            {/* Card 3: 7-Day Trend */}
                            <div className="bg-white/5 border border-white/10 rounded-xl p-6">
                                <div className="flex items-center gap-3 mb-4">
                                    <div className="p-2 bg-purple-500/20 rounded-lg text-purple-400">
                                        <BarChart3 size={20} />
                                    </div>
                                    <span className="text-slate-300">7-Day Trend</span>
                                </div>
                                <div className="flex items-end justify-between gap-1 h-16 mb-2">
                                    {[30, 45, 60, 40, 70, 55, 80].map((height, i) => (
                                        <div
                                            key={i}
                                            className="flex-1 bg-gradient-to-t from-purple-500 to-purple-400 rounded-t"
                                            style={{ height: `${height}%` }}
                                        />
                                    ))}
                                </div>
                                <div className="text-xs text-slate-400 text-center">Daily spending pattern</div>
                            </div>
                        </div>

                        {/* Analytics Preview Row */}
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                            {/* Donut Chart Preview */}
                            <div className="bg-white/5 border border-white/10 rounded-xl p-6">
                                <div className="flex items-center gap-2 mb-4">
                                    <div className="p-1.5 bg-blue-500/20 rounded-lg text-blue-400">
                                        <PieChart size={16} />
                                    </div>
                                    <span className="text-sm font-medium text-white">Category Breakdown</span>
                                </div>
                                <div className="flex items-center gap-4">
                                    <div className="relative w-24 h-24">
                                        <svg className="w-full h-full -rotate-90" viewBox="0 0 100 100">
                                            <circle cx="50" cy="50" r="35" fill="none" stroke="#ef4444" strokeWidth="14" strokeDasharray="70 220" strokeDashoffset="0" />
                                            <circle cx="50" cy="50" r="35" fill="none" stroke="#f59e0b" strokeWidth="14" strokeDasharray="50 220" strokeDashoffset="-70" />
                                            <circle cx="50" cy="50" r="35" fill="none" stroke="#10b981" strokeWidth="14" strokeDasharray="45 220" strokeDashoffset="-120" />
                                            <circle cx="50" cy="50" r="35" fill="none" stroke="#3b82f6" strokeWidth="14" strokeDasharray="55 220" strokeDashoffset="-165" />
                                            <circle cx="50" cy="50" r="25" fill="rgb(15, 23, 42)" />
                                        </svg>
                                    </div>
                                    <div className="flex-1 space-y-2">
                                        {[
                                            { color: 'bg-rose-500', name: 'Food', amount: '42%' },
                                            { color: 'bg-amber-500', name: 'Transport', amount: '23%' },
                                            { color: 'bg-emerald-500', name: 'Bills', amount: '20%' },
                                            { color: 'bg-blue-500', name: 'Other', amount: '15%' }
                                        ].map((cat, i) => (
                                            <div key={i} className="flex items-center gap-2 text-xs">
                                                <div className={`w-2 h-2 rounded-full ${cat.color}`} />
                                                <span className="text-slate-400">{cat.name}</span>
                                                <span className="ml-auto text-white font-medium">{cat.amount}</span>
                                            </div>
                                        ))}
                                    </div>
                                </div>
                            </div>

                            {/* Top Spending Categories */}
                            <div className="bg-white/5 border border-white/10 rounded-xl p-6">
                                <div className="flex items-center gap-2 mb-4">
                                    <span className="text-sm font-medium text-white">Top Spending</span>
                                </div>
                                <div className="space-y-3">
                                    {[
                                        { icon: '🍔', name: 'Food & Drink', amount: 78500, color: 'from-rose-500 to-pink-500' },
                                        { icon: '🚕', name: 'Transport', amount: 42300, color: 'from-amber-500 to-orange-500' },
                                        { icon: '💡', name: 'Utilities', amount: 37000, color: 'from-blue-500 to-cyan-500' }
                                    ].map((cat, i) => (
                                        <div key={i} className="space-y-1">
                                            <div className="flex items-center justify-between text-sm">
                                                <div className="flex items-center gap-2">
                                                    <span>{cat.icon}</span>
                                                    <span className="text-slate-300">{cat.name}</span>
                                                </div>
                                                <span className="text-white font-medium">₦{cat.amount.toLocaleString()}</span>
                                            </div>
                                            <div className="h-1.5 bg-slate-700 rounded-full overflow-hidden">
                                                <div className={`h-full bg-gradient-to-r ${cat.color} rounded-full`} style={{ width: `${85 - (i * 15)}%` }} />
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </section>

            {/* Feature 0: Advanced Analytics - NEW */}
            <section className="py-24 px-6">
                <div className="max-w-7xl mx-auto flex flex-col md:flex-row items-center gap-16">
                    <div className="flex-1 space-y-6">
                        <div className="w-14 h-14 rounded-xl bg-gradient-to-br from-purple-500/20 to-blue-500/20 flex items-center justify-center text-purple-400 mb-4">
                            <BarChart3 size={32} />
                        </div>
                        <h2 className="text-3xl md:text-4xl font-bold">Powerful Analytics Dashboard</h2>
                        <p className="text-xl text-slate-400 leading-relaxed">
                            Understand your spending like never before. Visualize patterns, compare periods, and make data-driven decisions with beautiful charts and insights.
                        </p>
                        <ul className="space-y-4 pt-4">
                            {[
                                'Interactive donut charts showing category breakdown',
                                '7-day and monthly spending trend analysis',
                                'Month-over-month comparisons with percentage changes',
                                'Top spending categories with visual progress bars',
                                'Real-time filtering by date, category, and transaction type'
                            ].map((item, i) => (
                                <li key={i} className="flex items-center gap-3 text-slate-300">
                                    <CheckCircle2 className="text-purple-400" size={20} />
                                    {item}
                                </li>
                            ))}
                        </ul>
                    </div>
                    <div className="flex-1 w-full relative">
                        <div className="absolute -inset-4 bg-gradient-to-r from-purple-500/20 to-blue-500/20 rounded-full blur-3xl opacity-20" />
                        <div className="relative bg-dark-900 border border-white/10 rounded-2xl p-6 shadow-2xl space-y-4">
                            {/* Mini Bar Chart */}
                            <div className="bg-white/5 p-4 rounded-xl border border-white/5">
                                <div className="text-sm font-medium mb-3 flex items-center justify-between">
                                    <span className="text-slate-300">Spending Over Time</span>
                                    <span className="text-xs text-emerald-400">↓ -8%</span>
                                </div>
                                <div className="flex items-end gap-2 h-24">
                                    {[60, 75, 45, 85, 65, 70, 55, 80, 60, 70].map((h, i) => (
                                        <div
                                            key={i}
                                            className="flex-1 bg-gradient-to-t from-primary to-secondary rounded-t hover:opacity-80 transition-opacity"
                                            style={{ height: `${h}%` }}
                                        />
                                    ))}
                                </div>
                                <div className="flex justify-between text-xs text-slate-500 mt-2">
                                    <span>Jan</span>
                                    <span>Dec</span>
                                </div>
                            </div>

                            {/* Comparison Cards */}
                            <div className="grid grid-cols-2 gap-3">
                                <div className="bg-emerald-500/10 border border-emerald-500/20 p-4 rounded-xl">
                                    <div className="text-xs text-slate-400 mb-1">This Month</div>
                                    <div className="text-xl font-bold text-white">₦152k</div>
                                    <div className="text-xs text-emerald-400 mt-1">↓ 12% vs last</div>
                                </div>
                                <div className="bg-blue-500/10 border border-blue-500/20 p-4 rounded-xl">
                                    <div className="text-xs text-slate-400 mb-1">Avg Transaction</div>
                                    <div className="text-xl font-bold text-white">₦4.2k</div>
                                    <div className="text-xs text-blue-400 mt-1">158 transactions</div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </section>

            {/* Feature 1: Smart Budgeting */}
            <section className="py-24 px-6 bg-dark-800/50">
                <div className="max-w-7xl mx-auto flex flex-col md:flex-row-reverse items-center gap-16">
                    <div className="flex-1 space-y-6">
                        <div className="w-14 h-14 rounded-xl bg-primary/20 flex items-center justify-center text-primary mb-4">
                            <Wallet size={32} />
                        </div>
                        <h2 className="text-3xl md:text-4xl font-bold">Smart Budgeting</h2>
                        <p className="text-xl text-slate-400 leading-relaxed">
                            Stop guessing where your money goes. Create custom budgets for groceries, rent, entertainment, and more. We'll alert you when you're getting close to your limit.
                        </p>
                        <ul className="space-y-4 pt-4">
                            {[
                                'Visual progress bars for every category',
                                'Real-time "Over Budget" alerts',
                                'Weekly, monthly, or yearly budget cycles'
                            ].map((item, i) => (
                                <li key={i} className="flex items-center gap-3 text-slate-300">
                                    <CheckCircle2 className="text-primary" size={20} />
                                    {item}
                                </li>
                            ))}
                        </ul>
                    </div>
                    <div className="flex-1 w-full relative">
                        <div className="absolute -inset-4 bg-primary/20 rounded-full blur-3xl opacity-20" />
                        <div className="relative bg-dark-900 border border-white/10 rounded-2xl p-6 shadow-2xl">
                            {/* Mock Budget Cards */}
                            <div className="space-y-4">
                                <div className="bg-white/5 p-4 rounded-xl border border-white/5">
                                    <div className="flex justify-between mb-2">
                                        <div className="flex items-center gap-3">
                                            <span className="text-2xl">🥦</span>
                                            <span className="font-bold">Groceries</span>
                                        </div>
                                        <span className="text-emerald-400 bg-emerald-400/10 px-2 py-1 rounded text-xs">On Track</span>
                                    </div>
                                    <div className="flex justify-between text-sm text-slate-400 mb-2">
                                        <span>₦35,000 spent</span>
                                        <span>₦50,000 limit</span>
                                    </div>
                                    <div className="h-2 bg-slate-700 rounded-full overflow-hidden">
                                        <div className="h-full bg-emerald-500 w-[70%]" />
                                    </div>
                                </div>

                                <div className="bg-white/5 p-4 rounded-xl border border-white/5">
                                    <div className="flex justify-between mb-2">
                                        <div className="flex items-center gap-3">
                                            <span className="text-2xl">🏠</span>
                                            <span className="font-bold">Rent</span>
                                        </div>
                                        <span className="text-rose-400 bg-rose-400/10 px-2 py-1 rounded text-xs">Over Budget</span>
                                    </div>
                                    <div className="flex justify-between text-sm text-slate-400 mb-2">
                                        <span>₦1,200,000 spent</span>
                                        <span>₦1,000,000 limit</span>
                                    </div>
                                    <div className="h-2 bg-slate-700 rounded-full overflow-hidden">
                                        <div className="h-full bg-rose-500 w-full" />
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </section>

            {/* Feature 2: Seamless Banking */}
            <section className="py-24 px-6">
                <div className="max-w-7xl mx-auto flex flex-col md:flex-row items-center gap-16">
                    <div className="flex-1 space-y-6">
                        <div className="w-14 h-14 rounded-xl bg-orange-500/20 flex items-center justify-center text-orange-500 mb-4">
                            <TrendingDown size={32} />
                        </div>
                        <h2 className="text-3xl md:text-4xl font-bold">Seamless Logging</h2>
                        <p className="text-xl text-slate-400 leading-relaxed">
                            Log an expense and open your bank app instantly to make the transfer. No more switching back and forth or forgetting to log after payment.
                        </p>
                        <ul className="space-y-4 pt-4">
                            {[
                                'One-tap deep linking to GTWorld, Kuda, PalmPay, Opay, etc',
                                'Automatically copies account details',
                                'Never miss a payment again'
                            ].map((item, i) => (
                                <li key={i} className="flex items-center gap-3 text-slate-300">
                                    <CheckCircle2 className="text-orange-500" size={20} />
                                    {item}
                                </li>
                            ))}
                        </ul>
                    </div>
                    <div className="flex-1 w-full relative">
                        <div className="absolute -inset-4 bg-orange-500/20 rounded-full blur-3xl opacity-20" />
                        <div className="relative bg-dark-900 border border-white/10 rounded-2xl p-8 shadow-2xl max-w-sm mx-auto">
                            {/* Mock Success Modal */}
                            <div className="text-center">
                                <div className="w-16 h-16 bg-emerald-500/20 text-emerald-500 rounded-full flex items-center justify-center mx-auto mb-4">
                                    <CheckCircle2 size={32} />
                                </div>
                                <h3 className="text-xl font-bold mb-2">Expense Logged!</h3>
                                <p className="text-slate-400 mb-6">You logged ₦5,000 for Lunch.</p>

                                <div className="bg-orange-500 text-white p-4 rounded-xl flex items-center justify-between cursor-pointer hover:bg-orange-600 transition-colors">
                                    <div className="flex items-center gap-3">
                                        <div className="w-8 h-8 bg-white/20 rounded-lg flex items-center justify-center font-bold">GT</div>
                                        <span className="font-medium">Open GTWorld</span>
                                    </div>
                                    <ArrowRight size={20} />
                                </div>
                                <p className="text-xs text-slate-500 mt-4">Tap to open app and complete transfer</p>
                            </div>
                        </div>
                    </div>
                </div>
            </section>

            {/* Feature 3: Investment Tracking */}
            <section className="py-24 px-6 bg-dark-800/50">
                <div className="max-w-7xl mx-auto flex flex-col md:flex-row-reverse items-center gap-16">
                    <div className="flex-1 space-y-6">
                        <div className="w-14 h-14 rounded-xl bg-blue-500/20 flex items-center justify-center text-blue-500 mb-4">
                            <TrendingUp size={32} />
                        </div>
                        <h2 className="text-3xl md:text-4xl font-bold">Track Your Wealth</h2>
                        <p className="text-xl text-slate-400 leading-relaxed">
                            Keep an eye on your future. Track your emergency fund, retirement savings, and crypto investments all in one place.
                        </p>
                        <ul className="space-y-4 pt-4">
                            {[
                                'Visualize portfolio growth',
                                'Set target goals for each asset',
                                'Track progress towards financial freedom'
                            ].map((item, i) => (
                                <li key={i} className="flex items-center gap-3 text-slate-300">
                                    <CheckCircle2 className="text-blue-500" size={20} />
                                    {item}
                                </li>
                            ))}
                        </ul>
                    </div>
                    <div className="flex-1 w-full relative">
                        <div className="absolute -inset-4 bg-blue-500/20 rounded-full blur-3xl opacity-20" />
                        <div className="relative bg-dark-900 border border-white/10 rounded-2xl p-6 shadow-2xl">
                            {/* Mock Investment Cards */}
                            <div className="grid grid-cols-2 gap-4">
                                <div className="bg-white/5 p-4 rounded-xl border border-white/5 col-span-2">
                                    <div className="flex justify-between items-start mb-4">
                                        <div>
                                            <div className="text-slate-400 text-sm">Total Portfolio</div>
                                            <div className="text-2xl font-bold">₦4,250,000</div>
                                        </div>
                                        <div className="bg-emerald-500/20 text-emerald-500 px-2 py-1 rounded text-xs font-bold">+15%</div>
                                    </div>
                                    <div className="h-32 flex items-end gap-1">
                                        {[40, 60, 45, 70, 65, 85, 80].map((h, i) => (
                                            <div key={i} className="flex-1 bg-blue-500/50 rounded-t hover:bg-blue-500 transition-colors" style={{ height: `${h}%` }} />
                                        ))}
                                    </div>
                                </div>
                                <div className="bg-white/5 p-4 rounded-xl border border-white/5">
                                    <div className="text-2xl mb-2">🚑</div>
                                    <div className="font-medium text-sm">Emergency</div>
                                    <div className="text-slate-400 text-xs">₦500k / ₦1M</div>
                                    <div className="mt-2 h-1.5 bg-slate-700 rounded-full overflow-hidden">
                                        <div className="h-full bg-amber-500 w-1/2" />
                                    </div>
                                </div>
                                <div className="bg-white/5 p-4 rounded-xl border border-white/5">
                                    <div className="text-2xl mb-2">🚀</div>
                                    <div className="font-medium text-sm">Crypto</div>
                                    <div className="text-slate-400 text-xs">₦1.2M / ₦5M</div>
                                    <div className="mt-2 h-1.5 bg-slate-700 rounded-full overflow-hidden">
                                        <div className="h-full bg-purple-500 w-[24%]" />
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </section>

            {/* CTA Section */}
            <section className="py-20 px-6">
                <div className="max-w-4xl mx-auto text-center bg-gradient-to-br from-primary/20 to-secondary/20 border border-white/10 rounded-3xl p-12 relative overflow-hidden">
                    <div className="absolute top-0 left-0 w-full h-full bg-noise opacity-10" />
                    <h2 className="text-3xl md:text-5xl font-bold mb-6 relative z-10">Ready to take control?</h2>
                    <p className="text-xl text-slate-300 mb-8 max-w-2xl mx-auto relative z-10">
                        Join thousands of users who are mastering their finances with MeBudget's powerful analytics and intuitive tools.
                    </p>
                    <Link
                        to="/register"
                        className="inline-flex items-center gap-2 bg-white text-dark-900 px-8 py-4 rounded-xl font-bold text-lg hover:bg-slate-200 transition-all hover:scale-105 active:scale-95 relative z-10"
                    >
                        Create Free Account <ArrowRight size={20} />
                    </Link>
                </div>
            </section>

            {/* Footer */}
            <footer className="py-8 border-t border-white/5 text-center text-slate-500 text-sm">
                <div className="flex justify-center gap-6 mb-4">
                    <Link to="/privacy" className="hover:text-white transition-colors">Privacy Policy</Link>
                    <Link to="/terms" className="hover:text-white transition-colors">Terms of Use</Link>
                </div>
                <p>&copy; {new Date().getFullYear()} MeBudget. All rights reserved.</p>
            </footer>
        </div>
    );
}