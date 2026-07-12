import { rimraf } from 'rimraf';

async function globalSetup(): Promise<void> {
	// Remove reports from previous runs
	await rimraf('./logs');
	await rimraf('./html-reports');
}

export default globalSetup;
