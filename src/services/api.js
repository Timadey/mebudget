import { supabase } from '../lib/supabase';

export const api = {
    // Fetch all data
    // Fetch all data with improved context awareness
    getData: async ({ startDate, endDate } = {}) => {
        try {
            // Build queries
            let transactionsQuery = supabase
                .from('transactions')
                .select('*')
                .order('date', { ascending: false });

            // Fetch budgets if we have a date range
            let budgetsQuery = supabase.from('budgets').select('*');

            if (startDate && endDate) {
                transactionsQuery = transactionsQuery
                    .gte('date', startDate.toISOString())
                    .lte('date', endDate.toISOString());

                // Fetch budgets that overlap with the period
                // Logic: budget_start <= period_end AND budget_end >= period_start
                budgetsQuery = budgetsQuery
                    .lte('start_date', endDate.toISOString())
                    .gte('end_date', startDate.toISOString())
                    .order('created_at', { ascending: false }); // Prioritize newest
            }

            const [categoriesRes, transactionsRes, investmentsRes, budgetsRes] = await Promise.all([
                supabase.from('categories').select('*'),
                transactionsQuery,
                supabase.from('investments').select('*'),
                budgetsQuery
            ]);

            if (categoriesRes.error) throw categoriesRes.error;
            if (transactionsRes.error) throw transactionsRes.error;
            if (investmentsRes.error) throw investmentsRes.error;
            if (budgetsRes.error && budgetsRes.error.code !== '42P01') throw budgetsRes.error;

            const budgets = budgetsRes.data || [];

            // Transform data to match the app's expected structure
            const categories = categoriesRes.data.map(cat => {
                const spent = transactionsRes.data
                    .filter(t => t.category === cat.name && t.type === 'Expense')
                    .reduce((sum, t) => sum + Number(t.amount), 0);

                let activeBudget = cat.budget_limit;

                if (startDate && endDate) {
                    // Find a budget that matches this period.
                    // Due to timezone shifts or potential legacy data, we avoid strict equality.
                    // We look for a budget that starts roughly when we expect (within 24 hours)
                    // and ends roughly when we expect.
                    const periodBudget = budgets.find(b => {
                        if (b.category_id !== cat.id) return false;

                        const bStart = new Date(b.start_date).getTime();
                        const bEnd = new Date(b.end_date).getTime();
                        const reqStart = startDate.getTime();
                        const reqEnd = endDate.getTime();

                        const startDiff = Math.abs(bStart - reqStart);
                        const endDiff = Math.abs(bEnd - reqEnd);

                        // Allow 24 hours of drift (86400000 ms)
                        return startDiff < 86400000 && endDiff < 86400000;
                    });

                    if (periodBudget) {
                        activeBudget = periodBudget.amount;
                    }
                }

                return {
                    ...cat,
                    budgetlimit: activeBudget, // This is now context-aware
                    defaultLimit: cat.budget_limit, // Keep original for reference
                    currentspent: spent
                };
            });

            // Map investments to match app structure
            const investments = investmentsRes.data.map(inv => ({
                ...inv,
                targetamount: inv.target_amount,
                currentbalance: inv.current_balance
            }));

            return {
                categories,
                transactions: transactionsRes.data,
                investments
            };

        } catch (error) {
            console.error('Supabase API Error:', error);
            return { categories: [], transactions: [], investments: [] };
        }
    },

    // Log a new expense
    logExpense: async (expense) => {
        try {
            const { data, error } = await supabase
                .from('transactions')
                .insert([{
                    date: expense.date || new Date().toISOString(), // Allow custom date
                    amount: expense.amount,
                    payee: expense.payee,
                    description: expense.description,
                    category: expense.category,
                    type: expense.type || 'Expense'
                }])
                .select();

            if (error) throw error;
            return { status: 'success', data };
        } catch (error) {
            console.error('Supabase Log Error:', error);
            throw error;
        }
    },

    // --- Categories ---

    createCategory: async (category) => {
        try {
            const { data, error } = await supabase
                .from('categories')
                .insert([{
                    name: category.name,
                    type: category.type,
                    budget_limit: category.budgetLimit,
                    icon: category.icon
                }])
                .select();
            if (error) throw error;
            return { status: 'success', data };
        } catch (error) {
            console.error('Create Category Error:', error);
            throw error;
        }
    },

    updateCategory: async (id, updates) => {
        try {
            const { data, error } = await supabase
                .from('categories')
                .update({
                    name: updates.name,
                    type: updates.type,
                    budget_limit: updates.budgetLimit,
                    icon: updates.icon
                })
                .eq('id', id)
                .select();
            if (error) throw error;
            return { status: 'success', data };
        } catch (error) {
            console.error('Update Category Error:', error);
            throw error;
        }
    },

    deleteCategory: async (id) => {
        try {
            const { error } = await supabase
                .from('categories')
                .delete()
                .eq('id', id);
            if (error) throw error;
            return { status: 'success' };
        } catch (error) {
            console.error('Delete Category Error:', error);
            throw error;
        }
    },

    // --- Budgets (Period Specific) ---

    setBudget: async (categoryId, amount, startDate, endDate) => {
        try {
            // Check if exists
            const { data: existing } = await supabase
                .from('budgets')
                .select('id')
                .eq('category_id', categoryId)
                .eq('start_date', startDate.toISOString())
                .eq('end_date', endDate.toISOString())
                .single();

            let result;
            if (existing) {
                result = await supabase
                    .from('budgets')
                    .update({ amount })
                    .eq('id', existing.id);
            } else {
                result = await supabase
                    .from('budgets')
                    .insert({
                        category_id: categoryId,
                        amount,
                        start_date: startDate.toISOString(),
                        end_date: endDate.toISOString()
                    });
            }

            if (result.error) throw result.error;
            return { status: 'success' };
        } catch (error) {
            console.error('Set Budget Error:', error);
            throw error;
        }
    },

    // --- Investments ---

    createInvestment: async (investment) => {
        try {
            const { data, error } = await supabase
                .from('investments')
                .insert([{
                    category: investment.category,
                    target_amount: investment.targetAmount,
                    current_balance: investment.currentBalance
                }])
                .select();
            if (error) throw error;
            return { status: 'success', data };
        } catch (error) {
            console.error('Create Investment Error:', error);
            throw error;
        }
    },

    updateInvestment: async (id, updates) => {
        try {
            const { data, error } = await supabase
                .from('investments')
                .update({
                    category: updates.category,
                    target_amount: updates.targetAmount,
                    current_balance: updates.currentBalance
                })
                .eq('id', id)
                .select();
            if (error) throw error;
            return { status: 'success', data };
        } catch (error) {
            console.error('Update Investment Error:', error);
            throw error;
        }
    },

    deleteInvestment: async (id) => {
        try {
            const { error } = await supabase
                .from('investments')
                .delete()
                .eq('id', id);
            if (error) throw error;
            return { status: 'success' };
        } catch (error) {
            console.error('Delete Investment Error:', error);
            throw error;
        }
    }
};
