import type { Locator, Page } from '@playwright/test';
import { BasePage } from './BasePage';

export class ChallengingDomPage extends BasePage {
	readonly heading: Locator;
	readonly buttons: Locator;
	readonly table: Locator;
	readonly tableRows: Locator;
	readonly canvas: Locator;

	constructor(page: Page) {
		super(page);

		this.heading = page.locator('h3');
		this.buttons = page.locator('.large-2.columns a.button');
		this.table = page.locator('table');
		this.tableRows = page.locator('table tbody tr');
		this.canvas = page.locator('#canvas');
	}

	async goto(): Promise<void> {
		await this.page.goto('/challenging_dom');
	}

	async clickButtonByText(text: string): Promise<void> {
		await this.page.getByRole('link', { name: text }).first().click();
	}

	async getCellValue(rowIndex: number, colIndex: number): Promise<string> {
		const cell = this.tableRows.nth(rowIndex).locator('td').nth(colIndex);
		return await cell.innerText();
	}

	async getRowData(rowIndex: number): Promise<string[]> {
		const cells = this.tableRows.nth(rowIndex).locator('td');
		const count = await cells.count();
		const data: string[] = [];

		for (let i = 0; i < count; i++) {
			data.push(await cells.nth(i).innerText());
		}

		return data;
	}

	async clickRowAction(
		rowIndex: number,
		action: 'edit' | 'delete',
	): Promise<void> {
		const row = this.tableRows.nth(rowIndex);
		await row.getByRole('link', { name: action }).click();
	}

	async getTableHeaders(): Promise<string[]> {
		const headers = this.page.locator('table thead th');
		return await headers.allInnerTexts();
	}
}
