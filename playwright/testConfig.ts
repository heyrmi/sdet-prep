import dotenv from 'dotenv';

dotenv.config();

export const testConfig = {
	env: process.env.ENV || 'stage',

	uiUrl: process.env.UI_URL || 'https://the-internet.herokuapp.com',
	apiUrl: process.env.API_URL || 'https://httpbin.org',
	freeApiUrl: process.env.FREE_API_URL || 'https://api.freeapi.app/api/v1/',

	username: process.env.AUTH_USERNAME || 'admin',
	password: process.env.AUTH_PASSWORD || 'admin',

	loginUsername: process.env.LOGIN_USERNAME || 'tomsmith',
	loginPassword: process.env.LOGIN_PASSWORD || 'SuperSecretPassword!',
} as const;
