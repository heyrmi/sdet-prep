import type { Locator, Page } from '@playwright/test';
import { BasePage } from './BasePage';

export class DataTablePage extends BasePage {
	readonly heading: Locator;
	readonly example1Heading: Locator;
	readonly example2Heading: Locator;

	readonly table1: Locator;
	readonly table1Headers: Locator;
	readonly table1Rows: Locator;

	readonly table2: Locator;
	readonly table2Headers: Locator;
	readonly table2Rows: Locator;

	constructor(page: Page) {
		super(page);

		this.heading = page.locator('h3');
		this.example1Heading = page.locator('h4').nth(0);
		this.example2Heading = page.locator('h4').nth(1);

		this.table1 = page.locator('table').nth(0);
		this.table1Headers = this.table1.locator('thead th');
		this.table1Rows = this.table1.locator('tbody tr');

		this.table2 = page.locator('#table2');
		this.table2Headers = this.table2.locator('thead th');
		this.table2Rows = this.table2.locator('tbody tr');
	}

	async goto(): Promise<void> {
		await this.page.goto('/tables');
	}

	async getTableHeaders(tableNumber: 1 | 2): Promise<string[]> {
		const headers = tableNumber === 1 ? this.table1Headers : this.table2Headers;
		return await headers.allTextContents();
	}

	async clickHeaderToSort(
		tableNumber: 1 | 2,
		headerName: string,
	): Promise<void> {
		const headers = tableNumber === 1 ? this.table1Headers : this.table2Headers;
		return headers.filter({ hasText: headerName }).click();
	}

	async getRowCount(tableNumber: 1 | 2): Promise<number> {
		const rows = tableNumber === 1 ? this.table1Rows : this.table2Rows;
		return await rows.count();
	}

	async getRowData(tableNumber: 1 | 2, rowIndex: number): Promise<string[]> {
		const tableRows = tableNumber === 1 ? this.table1Rows : this.table2Rows;
		const row = tableRows.nth(rowIndex);
		const cells = row.locator('td');
		return cells.allTextContents();
	}

	async getColumnData(
		tableNumber: 1 | 2,
		columnIndex: number,
	): Promise<string[]> {
		const table = tableNumber === 1 ? this.table1 : this.table2;
		// CSS nth-child is 1-indexed
		const cells = table.locator(`tbody tr td:nth-child(${columnIndex + 1})`);
		return cells.allTextContents();
	}

	async getAllTableData(tableNumber: 1 | 2): Promise<string[][]> {
		const rows = tableNumber === 1 ? this.table1Rows : this.table2Rows;
		const rowCount = await rows.count();
		const tableData: string[][] = [];

		for (let i = 0; i < rowCount; i++) {
			const rowData = await this.getRowData(tableNumber, i);
			tableData.push(rowData);
		}
		return tableData;
	}

	async getRowByLastName(
		tableNumber: 1 | 2,
		lastName: string,
	): Promise<Locator> {
		const rows = tableNumber === 1 ? this.table1Rows : this.table2Rows;
		return rows.filter({
			has: this.page.locator('td').first().filter({ hasText: lastName }),
		});
	}

	async getEmailByLastName(
		tableNumber: 1 | 2,
		lastName: string,
	): Promise<string> {
		const row = await this.getRowByLastName(tableNumber, lastName);

		const emailCell = row.locator('td').nth(2);
		return (await emailCell.textContent()) || '';
	}

	async getAmountByLastName(
		tableNumber: 1 | 2,
		lastName: string,
	): Promise<number> {
		const row = await this.getRowByLastName(tableNumber, lastName);
		const dueCell = row.locator('td').nth(3);
		const dueText = (await dueCell.textContent()) || '$0.00';
		return parseFloat(dueText.replace('$', ''));
	}

	async clickEditOrDeleteForPerson(
		tableNumber: 1 | 2,
		lastName: string,
		action: 'edit' | 'delete',
	): Promise<void> {
		const row = await this.getRowByLastName(tableNumber, lastName);
		await row.getByRole('link', { name: action }).click();
	}

	async getActionLinksForPerson(
		tableNumber: 1 | 2,
		lastName: string,
	): Promise<string[]> {
		const row = await this.getRowByLastName(tableNumber, lastName);
		const links = row.locator('td').last().locator('a');
		return await links.allTextContents();
	}

	async isColumnSortedAscendingOrDescending(
		tableNumber: 1 | 2,
		columnIndex: number,
		order: 'ascending' | 'descending',
	): Promise<boolean> {
		const data = await this.getColumnData(tableNumber, columnIndex);
		const trimmed = data.map((s) => s.trim());

		const sorted = [...trimmed].sort((a, b) => a.localeCompare(b));

		if (order === 'descending') {
			sorted.reverse();
		}
		return trimmed.every((val, i) => val === sorted[i]);
	}
}
