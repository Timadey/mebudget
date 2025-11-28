import { supabase } from '../lib/supabase';

export const api = {
    // Fetch all data
    getData: async () => {
        try {
            const [categoriesRes, transactionsRes, investmentsRes] = await Promise.all([
                supabase.from('categories').select('*'),
                supabase.from('transactions').select('*').order('date', { ascending: false }),
                supabase.from('investments').select('*')
            ]);

            if (categoriesRes.error) throw categoriesRes.error;
            if (transactionsRes.error) throw transactionsRes.error;
            if (investmentsRes.error) throw investmentsRes.error;

            // Transform data to match the app's expected structure
            // We need to calculate 'currentspent' for categories based on transactions
            const categories = categoriesRes.data.map(cat => {
                const spent = transactionsRes.data
                    .filter(t => t.category === cat.name && t.type === 'Expense')
                    .reduce((sum, t) => sum + Number(t.amount), 0);

                return {
                    ...cat,
                    budgetlimit: cat.budget_limit, // Map snake_case to app's expected key
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
                    date: new Date().toISOString(),
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
