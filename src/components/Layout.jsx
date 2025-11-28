import React, { useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { LayoutDashboard, PieChart, TrendingUp, Settings, Menu, X } from 'lucide-react';
import clsx from 'clsx';

const NavItem = ({ to, icon: Icon, label, onClick }) => {
    const location = useLocation();
    const isActive = location.pathname === to;

    return (
        <Link
            to={to}
            onClick={onClick}
            className={clsx(
                'flex items-center gap-3 px-4 py-3 rounded-xl transition-all duration-200 group',
                isActive
                    ? 'bg-primary text-white shadow-lg shadow-primary/25'
                    : 'text-slate-400 hover:bg-white/5 hover:text-white'
            )}
        >
            <Icon size={20} className={clsx(isActive ? 'text-white' : 'text-slate-400 group-hover:text-white')} />
            <span className="font-medium">{label}</span>
        </Link>
    );
};

export default function Layout({ children }) {
    const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

    const navItems = [
        { to: '/', icon: LayoutDashboard, label: 'Dashboard' },
        { to: '/expenses', icon: PieChart, label: 'Expenses' },
        { to: '/investments', icon: TrendingUp, label: 'Investments' },
        { to: '/settings', icon: Settings, label: 'Settings' }
    ];

    return (
        <div className="min-h-screen bg-dark-900 text-slate-50 flex">
            {/* Desktop Sidebar */}
            <aside className="w-64 fixed h-full glass border-r border-white/5 flex flex-col p-6 hidden md:flex z-50">
                <div className="mb-10 flex items-center gap-3 px-2">
                    <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-primary to-secondary flex items-center justify-center font-bold text-white">
                        M
                    </div>
                    <h1 className="text-xl font-bold bg-clip-text text-transparent bg-gradient-to-r from-white to-slate-400">
                        MeBudget
                    </h1>
                </div>

                <nav className="space-y-2 flex-1">
                    {navItems.map((item) => (
                        <NavItem key={item.to} {...item} />
                    ))}
                </nav>

                <div className="mt-auto pt-6 border-t border-white/5">
                    <div className="flex items-center gap-3 px-2">
                        <div className="w-8 h-8 rounded-full bg-slate-700" />
                        <div className="flex-1 min-w-0">
                            <p className="text-sm font-medium text-white truncate">User</p>
                            <p className="text-xs text-slate-400 truncate">user@example.com</p>
                        </div>
                    </div>
                </div>
            </aside>

            {/* Mobile Header */}
            <div className="md:hidden fixed top-0 left-0 right-0 h-16 glass z-50 flex items-center justify-between px-4 border-b border-white/5">
                <div className="flex items-center gap-3">
                    <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-primary to-secondary flex items-center justify-center font-bold text-white">
                        M
                    </div>
                    <h1 className="text-lg font-bold">MeBudget</h1>
                </div>
                <button
                    onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
                    className="p-2 hover:bg-white/10 rounded-lg transition-colors"
                >
                    {mobileMenuOpen ? <X size={24} /> : <Menu size={24} />}
                </button>
            </div>

            {/* Mobile Drawer */}
            {mobileMenuOpen && (
                <>
                    {/* Backdrop */}
                    <div
                        className="md:hidden fixed inset-0 bg-black/50 z-40 backdrop-blur-sm"
                        onClick={() => setMobileMenuOpen(false)}
                    />
                    {/* Drawer */}
                    <aside className="md:hidden fixed top-16 left-0 bottom-0 w-64 glass border-r border-white/5 flex flex-col p-6 z-50 animate-in slide-in-from-left duration-300">
                        <nav className="space-y-2 flex-1">
                            {navItems.map((item) => (
                                <NavItem key={item.to} {...item} onClick={() => setMobileMenuOpen(false)} />
                            ))}
                        </nav>

                        <div className="mt-auto pt-6 border-t border-white/5">
                            <div className="flex items-center gap-3 px-2">
                                <div className="w-8 h-8 rounded-full bg-slate-700" />
                                <div className="flex-1 min-w-0">
                                    <p className="text-sm font-medium text-white truncate">User</p>
                                    <p className="text-xs text-slate-400 truncate">user@example.com</p>
                                </div>
                            </div>
                        </div>
                    </aside>
                </>
            )}

            {/* Main Content */}
            <main className="flex-1 md:ml-64 p-4 md:p-8 pt-20 md:pt-8 overflow-y-auto">
                <div className="max-w-7xl mx-auto">
                    {children}
                </div>
            </main>
        </div>
    );
}
