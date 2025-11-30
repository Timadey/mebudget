# MeBudget 💰

A premium personal budgeting and investment management application built with React and Supabase. Track your expenses, manage budgets, monitor investments, and secure your financial data with PIN protection.

![React](https://img.shields.io/badge/React-18.3-blue?logo=react)
![Vite](https://img.shields.io/badge/Vite-6.0-purple?logo=vite)
![TailwindCSS](https://img.shields.io/badge/TailwindCSS-4.1-cyan?logo=tailwindcss)
![Supabase](https://img.shields.io/badge/Supabase-PostgreSQL-green?logo=supabase)

## ✨ Features

### 💳 Expense Management
- **Budget Tracking**: Set budget limits for different expense categories
- **Real-time Monitoring**: Track spending with visual progress bars
- **Smart Alerts**: Color-coded warnings (Green: On Track, Amber: Critically Low, Red: Over Budget)
- **Transaction Logging**: Log both expenses and income with detailed information
- **Payment Integration**: Quick access to banking apps (GT World, UBA Mobile, Palmpay, Kuda, Parkway Wallet)

### 📊 Budget Duration
- **Flexible Periods**: Choose weekly, monthly, or yearly budget cycles
- **Period-based Calculations**: Automatic spending calculations based on selected duration
- **Budget Summary**: View total budget, total spent, and remaining balance for current period
- **Visual Progress**: Track budget usage with dynamic progress bars

### 📈 Investment Tracking
- **Portfolio Management**: Track multiple investments with targets
- **Progress Monitoring**: Visual representation of investment progress
- **CRUD Operations**: Create, edit, and delete investments
- **Total Portfolio Value**: See your complete investment portfolio at a glance

### 🔒 Security
- **PIN Protection**: Secure your app with a 4-digit PIN
- **Auto-lock**: Re-authentication required every 2 hours
- **Encrypted Storage**: PIN is hashed using bcrypt (never stored in plain text)
- **Session Management**: Secure session tracking in Supabase

### 🎨 Design
- **Premium UI**: Modern glassmorphism design with dark mode
- **Responsive**: Fully responsive design for mobile and desktop
- **Smooth Animations**: Micro-animations for enhanced user experience
- **Mobile Navigation**: Hamburger menu with slide-out drawer for mobile devices

## 🚀 Getting Started

### Prerequisites

- Node.js (v18 or higher)
- npm or yarn
- Supabase account

### Installation

1. **Clone the repository**
```bash
git clone <repository-url>
cd mebudget
```

2. **Install dependencies**
```bash
npm install
```

3. **Set up environment variables**

Create a `.env` file in the root directory:
```env
VITE_SUPABASE_URL=your_supabase_url
VITE_SUPABASE_ANON_KEY=your_supabase_anon_key
```

4. **Set up Supabase database**

   Run the SQL scripts located in the `schemas/` directory in your Supabase SQL Editor in the following order:

   1. `schemas/01_user_context.sql` - Sets up user context and RLS helper.
   2. `schemas/02_assign_data.sql` - Function to assign data to new users.
   3. `schemas/03_delete_account.sql` - Function to handle account deletion.
   4. `schemas/04_refactor_settings.sql` - Sets up the settings table structure.
   5. `schemas/05_default_categories.sql` - Sets up default categories trigger.
   6. `schemas/06_fix_settings_fk.sql` - Fixes foreign key constraints.
   7. `schemas/07_fix_trigger_column.sql` - Fixes trigger column names.

   > **Note**: You can copy the content of these files and paste them into the Supabase SQL Editor.

5. **Run the development server**
```bash
npm run dev
```

The app will be available at `http://localhost:5173`

## 📱 Usage

### First Time Setup

1. **Navigate to Settings**: Click on the Settings icon in the sidebar
2. **Set Budget Duration**: Choose weekly, monthly, or yearly budget cycle
3. **Enable PIN Protection** (Optional): Set a 4-digit PIN to secure your app

### Managing Categories

1. Go to **Expenses** page
2. Click **Manage Categories**
3. Create categories with:
   - Name
   - Type (Expense/Income/Investment)
   - Budget Limit
   - Icon (emoji)

### Logging Transactions

1. Go to **Expenses** page
2. Click **Log Transaction**
3. Toggle between Expense/Income
4. Fill in:
   - Amount
   - Category
   - Payee/Source
   - Description (optional)
   - Payment Source (optional - for expenses only)
5. Submit

### Managing Investments

1. Go to **Investments** page
2. Click **Manage Investments**
3. Add investments with:
   - Name
   - Target Amount
   - Current Balance

## 🛠️ Tech Stack

### Frontend
- **React 18.3** - UI library
- **Vite 6.0** - Build tool
- **React Router 7.1** - Routing
- **TailwindCSS 4.1** - Styling
- **Lucide React** - Icons
- **clsx** - Conditional styling

### Backend
- **Supabase** - PostgreSQL database
- **Supabase JS Client** - API integration
- **bcryptjs** - Password hashing

### Development
- **ESLint** - Code linting
- **PostCSS** - CSS processing

## 📁 Project Structure

```
mebudget/
├── src/
│   ├── components/          # Reusable components
│   │   ├── CategoryManager.jsx
│   │   ├── ExpenseForm.jsx
│   │   ├── InvestmentManager.jsx
│   │   ├── Layout.jsx
│   │   ├── Modal.jsx
│   │   └── PinModal.jsx
│   ├── pages/              # Page components
│   │   ├── Dashboard.jsx
│   │   ├── Expenses.jsx
│   │   ├── Investments.jsx
│   │   └── Settings.jsx
│   ├── services/           # API services
│   │   ├── api.js
│   │   └── settings.js
│   ├── lib/                # Utilities
│   │   └── supabase.js
│   ├── styles/             # Global styles
│   │   └── index.css
│   ├── App.jsx             # Main app component
│   └── main.jsx            # Entry point
├── schemas/                # Database schemas
│   ├── schema.sql
│   └── settings.sql
├── .env                    # Environment variables (gitignored)
├── .env.example            # Environment template
└── package.json
```

## 🎨 Customization

### Currency Symbol

The app uses Nigerian Naira (₦) by default. To change the currency symbol, update all instances of `₦` in the following files:
- `src/pages/Dashboard.jsx`
- `src/pages/Expenses.jsx`
- `src/pages/Investments.jsx`
- `src/components/CategoryManager.jsx`
- `src/components/InvestmentManager.jsx`

### Theme Colors

Edit `src/styles/index.css` to customize colors:
```css
@theme {
  --color-primary: #8b5cf6;      /* Purple */
  --color-secondary: #ec4899;    /* Pink */
  --color-dark-900: #0f172a;     /* Dark background */
}
```

### Banking Apps

Edit `src/components/ExpenseForm.jsx` to add/modify banking app integrations:
```javascript
const PAYMENT_APPS = [
  { 
    id: 'your-bank', 
    name: 'Your Bank', 
    color: 'bg-blue-500',
    androidScheme: 'yourbank://',
    iosScheme: 'yourbank://',
    fallbackUrl: 'https://play.google.com/store/apps/details?id=com.yourbank.app'
  }
];
```

## 🔐 Security Best Practices

1. **Never commit `.env` file** - It's already in `.gitignore`
2. **Use Row Level Security (RLS)** in Supabase for production
3. **Enable Supabase Auth** for multi-user support
4. **Regularly update dependencies** for security patches

## 🚢 Deployment

### Vercel (Recommended)

1. Push your code to GitHub
2. Import project in Vercel
3. Add environment variables in Vercel dashboard
4. Deploy

### Netlify

1. Build the project: `npm run build`
2. Deploy the `dist` folder to Netlify
3. Add environment variables in Netlify dashboard

## 📝 License

This project is licensed under the MIT License.

## 🤝 Contributing

Contributions are welcome! Please read our [Contributing Guide](CONTRIBUTING.md) for details on our code of conduct, and the process for submitting pull requests.
## 📧 Support

For issues and questions, please open an issue on GitHub.

Contact me via email [dlktimothy@gmail.com](mailto:dlktimothy@gmail.com) or via LinkedIn [linkedin/in/timadey](https://www.linkedin.com/in/timadey)

---

**Made with ❤️ using React and Supabase**
