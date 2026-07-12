import test from '@lib/BaseTest';
import { expect } from '@playwright/test';
import tableData from '@testdata/ui/table-data.json' with { type: 'json' };
import type { TableTestData } from '@/types/testData';

test.describe('Data Table Page Test', () => {
	test.beforeEach(async ({ dataTablePage }) => {
		await dataTablePage.goto();
	});

	test.describe('Page Structure', () => {
		test('should display the heading and examples @smoke', async ({
			dataTablePage,
		}) => {
			await expect(dataTablePage.page).toHaveTitle('The Internet');
			await expect(dataTablePage.heading).toHaveText('Data Tables');
			await expect(dataTablePage.example1Heading).toHaveText('Example 1');
			await expect(dataTablePage.example2Heading).toHaveText('Example 2');
			await expect(dataTablePage.page).toHaveURL(/.*\/tables/);
		});
	});
	test.describe('Table Structure', () => {
		test('should have correct headers in Table 1', async ({
			dataTablePage,
		}) => {
			const headers = await dataTablePage.getTableHeaders(1);
			expect(headers).toEqual([
				'Last Name',
				'First Name',
				'Email',
				'Due',
				'Web Site',
				'Action',
			]);
		});

		test('should have correct headers in Table 2', async ({
			dataTablePage,
		}) => {
			const headers = await dataTablePage.getTableHeaders(2);
			expect(headers).toHaveLength(6);
			expect(headers).toContain('Email');
			expect(headers).toContain('Action');
		});

		test('should have 4 data rows in each table', async ({ dataTablePage }) => {
			const table1Rows = await dataTablePage.getRowCount(1);
			const table2Rows = await dataTablePage.getRowCount(2);

			expect(table1Rows).toBe(4);
			expect(table2Rows).toBe(4);
		});

		test('headers should be visible', async ({ dataTablePage }) => {
			await expect(dataTablePage.table1Headers.first()).toBeVisible();
			await expect(dataTablePage.table2Headers.first()).toBeVisible();
		});
	});

	test.describe('Table data verification', () => {
		test('should contain expected last names', async ({ dataTablePage }) => {
			const lastNames = await dataTablePage.getColumnData(1, 0);

			expect(lastNames).toContain('Smith');
			expect(lastNames).toContain('Bach');
			expect(lastNames).toContain('Doe');
			expect(lastNames).toContain('Conway');
		});

		test('should have correct email format', async ({ dataTablePage }) => {
			const dueAmounts = await dataTablePage.getColumnData(1, 3);
			dueAmounts.forEach((amount) => {
				expect(amount).toMatch(/^\$\d+\.\d{2}$/);
			});
		});

		test('should get correct row data', async ({ dataTablePage }) => {
			const firstRow = await dataTablePage.getRowData(1, 0);
			expect(firstRow).toHaveLength(6);
			expect(firstRow[0]).toBeTruthy();
			expect(firstRow[1]).toBeTruthy();
		});

		test('should get all table data', async ({ dataTablePage }) => {
			const tableData = await dataTablePage.getAllTableData(1);

			expect(tableData).toHaveLength(4);
			expect(tableData[0]).toHaveLength(6);
		});
	});

	test.describe('Finding specific data', () => {
		test('should find email by last name', async ({ dataTablePage }) => {
			const email = await dataTablePage.getEmailByLastName(1, 'Smith');
			expect(email).toBe('jsmith@gmail.com');
		});

		test('should find due amount by last name', async ({ dataTablePage }) => {
			const dueAmount = await dataTablePage.getAmountByLastName(1, 'Doe');
			expect(dueAmount).toBe(100.0);
		});

		test('should find the person with highest due amount', async ({
			dataTablePage,
		}) => {
			const allData = await test.step('Retrieve all table data', async () => {
				return await dataTablePage.getAllTableData(1);
			});

			const { personWithMaxDue, maxDue } =
				await test.step('Find the person with max due', () => {
					let maxDue = 0;
					let personWithMaxDue = '';

					allData.forEach((row) => {
						const due = parseFloat(row[3].replace('$', ''));
						if (due > maxDue) {
							maxDue = due;
							personWithMaxDue = row[0];
						}
					});

					return { personWithMaxDue, maxDue };
				});

			expect(personWithMaxDue).toBe('Doe');
			expect(maxDue).toBe(100.0);
		});
	});

	test.describe('Table Sorting', () => {
		test('should sort by Last Name ascending when clicked', async ({
			dataTablePage,
		}) => {
			await dataTablePage.clickHeaderToSort(2, 'Last Name');

			const lastNames = await dataTablePage.getColumnData(2, 0);

			const sortedNames = [...lastNames].sort();
			expect(lastNames).toEqual(sortedNames);
		});

		test('should sort by Last Name descending when clicked twice', async ({
			dataTablePage,
		}) => {
			await dataTablePage.clickHeaderToSort(2, 'Last Name');
			await dataTablePage.clickHeaderToSort(2, 'Last Name');

			const lastNames = await dataTablePage.getColumnData(2, 0);
			const sortedDescending = [...lastNames].sort().reverse();

			expect(lastNames).toEqual(sortedDescending);
		});

		test('should sort by First Name', async ({ dataTablePage }) => {
			await dataTablePage.clickHeaderToSort(2, 'First Name');

			const firstName = await dataTablePage.getColumnData(2, 1);
			const sorted = [...firstName].sort();

			expect(firstName).toEqual(sorted);
		});

		test('should sort by Email', async ({ dataTablePage }) => {
			await dataTablePage.clickHeaderToSort(2, 'Email');

			const emails = await dataTablePage.getColumnData(2, 2);
			const sorted = [...emails].sort();

			expect(emails).toEqual(sorted);
		});

		test('should verify column sort state using helper method', async ({
			dataTablePage,
		}) => {
			await dataTablePage.clickHeaderToSort(2, 'Last Name');

			const isSortedAsc =
				await dataTablePage.isColumnSortedAscendingOrDescending(
					2,
					0,
					'ascending',
				);

			expect(isSortedAsc).toBe(true);
		});
	});

	test.describe('Action Buttons', () => {
		test('each row should have edit and delete links', async ({
			dataTablePage,
		}) => {
			const actions = await dataTablePage.getActionLinksForPerson(1, 'Smith');

			expect(actions).toContain('edit');
			expect(actions).toContain('delete');
		});
	});

	test('edit link should be clickable', async ({ dataTablePage }) => {
		await dataTablePage.clickEditOrDeleteForPerson(1, 'Bach', 'edit');
		await expect(dataTablePage.page).toHaveURL(/.*#edit/);
	});

	test('delete link should be clickable', async ({ dataTablePage }) => {
		await dataTablePage.clickEditOrDeleteForPerson(1, 'Bach', 'delete');
		await expect(dataTablePage.page).toHaveURL(/.*#delete/);
	});

	test('all rows should have action links', async ({ dataTablePage }) => {
		const tableNumber = 1;
		const rowCount = await dataTablePage.getRowCount(tableNumber);
		for (let i = 0; i < rowCount; i++) {
			const rowData = await dataTablePage.getRowData(tableNumber, i);
			const actionCell = rowData[5];

			expect(actionCell).toContain('edit');
			expect(actionCell).toContain('delete');
		}
	});

	test.describe('Soft assertions', () => {
		test('verify multiple fields with soft assertions', async ({
			dataTablePage,
		}) => {
			const tableNumber = 1;
			const allData = await dataTablePage.getAllTableData(tableNumber);

			for (const row of allData) {
				expect.soft(row[0]).toBeTruthy();
				expect.soft(row[1]).toBeTruthy();
				expect.soft(row[2]).toContain('@');
				expect.soft(row[3]).toMatch(/^\$/);
			}
		});
	});

	test.describe('Parameterized Test', () => {
		for (const data of tableData as TableTestData[]) {
			test(`should have correct data for ${data.lastName}`, async ({
				dataTablePage,
			}) => {
				const tableNumber = 1;
				const email = await dataTablePage.getEmailByLastName(
					tableNumber,
					data.lastName,
				);
				expect(email).toBe(data.expectedEmail);
			});
		}

		const tableNumbers: (1 | 2)[] = [1, 2];
		for (const tableNum of tableNumbers) {
			test(`table ${tableNum} should have 4 rows`, async ({
				dataTablePage,
			}) => {
				const rowCount = await dataTablePage.getRowCount(tableNum);
				expect(rowCount).toBe(4);
			});
		}
	});
});
