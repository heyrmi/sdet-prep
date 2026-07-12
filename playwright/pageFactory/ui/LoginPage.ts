import type { Locator, Page } from '@playwright/test';
import { BasePage } from './BasePage';

export class LoginPage extends BasePage {
	readonly heading: Locator;
	readonly subHeading: Locator;
	readonly usernameInput: Locator;
	readonly passwordInput: Locator;
	readonly loginButton: Locator;
	readonly flashMessage: Locator;

	constructor(page: Page) {
		super(page);

		this.heading = page.locator('h2');
		this.subHeading = page.locator('h4');
		this.usernameInput = page.locator('#username');
		this.passwordInput = page.locator('#password');
		this.loginButton = page.locator("button[type='submit']");
		this.flashMessage = page.locator('#flash');
	}

	async goto(): Promise<void> {
		await this.page.goto('/login');
	}

	async login(username: string, password: string): Promise<void> {
		await this.usernameInput.fill(username);
		await this.passwordInput.fill(password);
		await this.loginButton.click();
	}
}
