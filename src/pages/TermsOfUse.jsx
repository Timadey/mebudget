import React from 'react';
import { Link } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';

export default function TermsOfUse() {
    return (
        <div className="min-h-screen bg-dark-900 text-white p-6 md:p-12">
            <div className="max-w-4xl mx-auto">
                <Link to="/" className="inline-flex items-center gap-2 text-slate-400 hover:text-white mb-8 transition-colors">
                    <ArrowLeft size={20} /> Back to Home
                </Link>

                <h1 className="text-4xl font-bold mb-8">Terms of Use</h1>

                <div className="bg-blue-500/10 border border-blue-500/20 text-blue-200 p-4 rounded-xl mb-8 text-sm">
                    <strong>Open Source Note:</strong> This is a template terms of use for the hosted version of MeBudget.
                    If you are self-hosting this application, please replace this content with your own terms.
                </div>

                <div className="space-y-6 text-slate-300 leading-relaxed">
                    <p>Last updated: {new Date().toLocaleDateString()}</p>

                    <section>
                        <h2 className="text-2xl font-bold text-white mb-4">1. Agreement to Terms</h2>
                        <p>By accessing our website, you agree to be bound by these Terms of Use and agree that you are responsible for the agreement with any applicable local laws. If you disagree with any of these terms, you are prohibited from accessing this site.</p>
                    </section>

                    <section>
                        <h2 className="text-2xl font-bold text-white mb-4">2. Use License</h2>
                        <p>Permission is granted to temporarily download one copy of the materials on MeBudget's website for personal, non-commercial transitory viewing only. This is the grant of a license, not a transfer of title, and under this license you may not:</p>
                        <ul className="list-disc pl-6 mt-2 space-y-2">
                            <li>modify or copy the materials;</li>
                            <li>use the materials for any commercial purpose or for any public display;</li>
                            <li>attempt to reverse engineer any software contained on MeBudget's website;</li>
                            <li>remove any copyright or other proprietary notations from the materials; or</li>
                            <li>transfer the materials to another person or "mirror" the materials on any other server.</li>
                        </ul>
                    </section>

                    <section>
                        <h2 className="text-2xl font-bold text-white mb-4">3. Disclaimer</h2>
                        <p>The materials on MeBudget's website are provided on an 'as is' basis. MeBudget makes no warranties, expressed or implied, and hereby disclaims and negates all other warranties including, without limitation, implied warranties or conditions of merchantability, fitness for a particular purpose, or non-infringement of intellectual property or other violation of rights.</p>
                    </section>

                    <section>
                        <h2 className="text-2xl font-bold text-white mb-4">4. Limitations</h2>
                        <p>In no event shall MeBudget or its suppliers be liable for any damages (including, without limitation, damages for loss of data or profit, or due to business interruption) arising out of the use or inability to use the materials on MeBudget's website, even if MeBudget or a MeBudget authorized representative has been notified orally or in writing of the possibility of such damage.</p>
                    </section>
                </div>
            </div>
        </div>
    );
}
