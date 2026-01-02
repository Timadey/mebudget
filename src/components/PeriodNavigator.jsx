import React from 'react';
import { ChevronLeft, ChevronRight, Calendar } from 'lucide-react';

export default function PeriodNavigator({ period, onPeriodChange, duration = 'monthly' }) {
    const handlePrevious = () => {
        const newStart = new Date(period.start);
        const newEnd = new Date(period.end);

        if (duration === 'weekly') {
            newStart.setUTCDate(newStart.getUTCDate() - 7);
            newEnd.setUTCDate(newEnd.getUTCDate() - 7);
        } else if (duration === 'monthly') {
            newStart.setUTCMonth(newStart.getUTCMonth() - 1);
            // Re-calculate end of month to handle different lengths
            const nextMonth = new Date(newStart);
            nextMonth.setUTCMonth(nextMonth.getUTCMonth() + 1);
            nextMonth.setUTCDate(0);
            nextMonth.setUTCHours(23, 59, 59, 999);
            newEnd.setTime(nextMonth.getTime());
        } else if (duration === 'yearly') {
            newStart.setUTCFullYear(newStart.getUTCFullYear() - 1);
            newEnd.setUTCFullYear(newEnd.getUTCFullYear() - 1);
        }

        onPeriodChange({ start: newStart, end: newEnd });
    };

    const handleNext = () => {
        const newStart = new Date(period.start);
        const newEnd = new Date(period.end);

        if (duration === 'weekly') {
            newStart.setUTCDate(newStart.getUTCDate() + 7);
            newEnd.setUTCDate(newEnd.getUTCDate() + 7);
        } else if (duration === 'monthly') {
            newStart.setUTCMonth(newStart.getUTCMonth() + 1);
            // Re-calculate end of month
            const nextMonth = new Date(newStart);
            nextMonth.setUTCMonth(nextMonth.getUTCMonth() + 1);
            nextMonth.setUTCDate(0);
            nextMonth.setUTCHours(23, 59, 59, 999);
            newEnd.setTime(nextMonth.getTime());
        } else if (duration === 'yearly') {
            newStart.setUTCFullYear(newStart.getUTCFullYear() + 1);
            newEnd.setUTCFullYear(newEnd.getUTCFullYear() + 1);
        }

        onPeriodChange({ start: newStart, end: newEnd });
    };

    const formatDateRange = () => {
        const options = { month: 'short', year: 'numeric' };
        if (duration === 'weekly') {
            return `${period.start.toLocaleDateString()} - ${period.end.toLocaleDateString()}`;
        } else if (duration === 'monthly') {
            return period.start.toLocaleDateString('en-US', { month: 'long', year: 'numeric' });
        } else if (duration === 'yearly') {
            return period.start.getFullYear().toString();
        }
        return '';
    };

    return (
        <div className="bg-white/5 rounded-xl border border-white/10 p-2 flex items-center justify-between w-full sm:w-auto min-w-[200px]">
            <button
                onClick={handlePrevious}
                className="p-2 hover:bg-white/10 rounded-lg text-slate-400 hover:text-white transition-colors"
            >
                <ChevronLeft size={20} />
            </button>

            <div className="flex items-center gap-2 text-white font-medium">
                <Calendar size={16} className="text-primary" />
                <span>{formatDateRange()}</span>
            </div>

            <button
                onClick={handleNext}
                className="p-2 hover:bg-white/10 rounded-lg text-slate-400 hover:text-white transition-colors"
            >
                <ChevronRight size={20} />
            </button>
        </div>
    );
}
