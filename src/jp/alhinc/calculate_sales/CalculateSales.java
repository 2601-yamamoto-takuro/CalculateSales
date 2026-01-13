package jp.alhinc.calculate_sales;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CalculateSales {

	// 支店定義ファイル名
	private static final String FILE_NAME_BRANCH_LST = "branch.lst";

	// 支店別集計ファイル名
	private static final String FILE_NAME_BRANCH_OUT = "branch.out";

	// 商品定義ファイル名
	private static final String FILE_NAME_COMMODITY_LST = "commodity.lst";

	// 商品別集計ファイル名
	private static final String FILE_NAME_COMMODITY_OUT = "commodity.out";

	// 支店コードチェック
	private static final String BRANCH_REGEX = "^[0-9]{3}$";

	// 商品コードチェック
	private static final String COMMODITY_REGEX = "^[a-zA-Z0-9]{8}$";

	// エラーメッセージ
	private static final String UNKNOWN_ERROR = "予期せぬエラーが発生しました";
	private static final String BRANCH_FILE_DEFINED = "支店定義ファイル";
	private static final String COMMODITY_FILE_DEFINED = "商品定義ファイル";
	private static final String FILE_NOT_EXIST = "が存在しません";
	private static final String FILE_INVALID_FORMAT = "のフォーマットが不正です";
	private static final String SALES_FILE_NUMBER_NOT_CONSECUTIVE = "売上ファイル名が連番になっていません";
	private static final String AMOUNT_OVER_TEN_DIGITS = "合計金額が10桁を超えました";
	private static final String BRANCH_CODE_INVALID = "の支店コードが不正です";
	private static final String COMMODITY_CODE_INVALID = "の商品コードが不正です";
	/**
	 * メインメソッド
	 *
	 * @param コマンドライン引数
	 */
	public static void main(String[] args) {
		// コマンドライン引数の確認
		if (args.length != 1) {
			System.out.println(UNKNOWN_ERROR);
			return;
		}

		// 支店コードと支店名を保持するMap
		Map<String, String> branchNames = new HashMap<>();
		// 支店コードと売上金額を保持するMap
		Map<String, Long> branchSales = new HashMap<>();

		// 商品コードと商品名を保持するMap
		Map<String, String> commodityNames = new HashMap<>();
		// 商品コードと売上金額を保持するMap
		Map<String, Long> commoditySales = new HashMap<>();

		// 支店定義ファイル読み込み処理
		if(!readFile(args[0], FILE_NAME_BRANCH_LST, branchNames, branchSales,
					  BRANCH_REGEX, BRANCH_FILE_DEFINED)) {
			return;
		}


		// 商品定義ファイル読み込み処理
		if(!readFile(args[0], FILE_NAME_COMMODITY_LST, commodityNames, commoditySales,
					  COMMODITY_REGEX, COMMODITY_FILE_DEFINED)) {
			return;
		}

		// ※ここから集計処理を作成してください。(処理内容2-1、2-2)
		String path = args[0];
		File[] files = new File(path).listFiles();

		List<File> rcdFiles = new ArrayList<>();

		for (int i = 0; i < files.length; i++) {
			if (files[i].isFile() && files[i].getName().matches("^[0-9]{8}.rcd$")) {
				rcdFiles.add(files[i]);
			}
		}

		Collections.sort(rcdFiles);
		// 売上ファイルが連番になっているか
		for(int i = 0; i < rcdFiles.size() -1; i++) {

			int former = Integer.parseInt(rcdFiles.get(i).getName().substring(0, 8));
			int latter = Integer.parseInt(rcdFiles.get(i + 1).getName().substring(0, 8));

			if((latter - former) != 1) {
				System.out.println(SALES_FILE_NUMBER_NOT_CONSECUTIVE);
				return;
			}
		}

		for (int i = 0; i < rcdFiles.size(); i++) {
			BufferedReader br = null;

			try {
				String fileName = rcdFiles.get(i).getName();
				File file = new File(path, fileName);
				FileReader fr = new FileReader(file);
				br = new BufferedReader(fr);

				String line;
				// 保持用
				List<String> salesList = new ArrayList<>();

				while((line = br.readLine()) != null) {
					salesList.add(line);
				}

				// 売上ファイルのフォーマットが3行か
				if (salesList.size() != 3) {
					System.out.println(fileName + FILE_INVALID_FORMAT);
					return;
				}

				// 支店コードが有効か
				if (!branchNames.containsKey(salesList.get(0))) {
					System.out.println(fileName + BRANCH_CODE_INVALID);
					return;
				}

				// 商品コードが有効か
				if (!commodityNames.containsKey(salesList.get(1))) {
					System.out.println(fileName + COMMODITY_CODE_INVALID);
					return;
				}

				// 売上金額が数値か確認
				if (!salesList.get(2).matches("^[0-9]+$")) {
					System.out.println(UNKNOWN_ERROR);
					return;
				}

				// 型変換
				Long fileSale = Long.parseLong(salesList.get(2));

				// 支店別売上
				String branchKey = salesList.get(0);
				Long totalBranchSales = branchSales.get(branchKey) + fileSale;
				// 商品別売上
				String commodityKey = salesList.get(1);
				Long totalCommoditySales = commoditySales.get(commodityKey) + fileSale;

				// 売上金額の桁数チェック
				if (totalBranchSales >= 10000000000L || totalCommoditySales >= 10000000000L) {
					System.out.println(AMOUNT_OVER_TEN_DIGITS);
					return;
				}
				branchSales.put(branchKey, totalBranchSales);
				commoditySales.put(commodityKey, totalCommoditySales);

			} catch(IOException e) {
				System.out.println(UNKNOWN_ERROR);
				return;
			} finally {
				// ファイルを開いている場合
				if(br != null) {
					try {
						// ファイルを閉じる
						br.close();
					} catch(IOException e) {
						System.out.println(UNKNOWN_ERROR);
						return;
					}
				}
			}
		}


		// 支店別集計ファイル書き込み処理
		if(!writeFile(args[0], FILE_NAME_BRANCH_OUT, branchNames, branchSales)) {
			return;
		}

		// 商品別集計ファイル書き込み処理
		if(!writeFile(args[0], FILE_NAME_COMMODITY_OUT, commodityNames, commoditySales)) {
			return;
		}

	}

	/**
	 * 定義ファイル読み込み処理
	 *
	 * @param フォルダパス
	 * @param ファイル名
	 * @param 支店コードと支店名を保持するMap
	 * @param 支店コードと売上金額を保持するMap
	 * @param フォーマット用正規表現
	 * @param エラーメッセージ用ファイル名
	 * @return 読み込み可否
	 */
	private static boolean readFile(String path, String fileName, Map<String, String> branchNames, Map<String, Long> branchSales,
			String codeRegex, String errorfileName
			) {
		BufferedReader br = null;

		try {
			File file = new File(path, fileName);
			// 定義ファイルの存在チェック
			if (!file.exists()) {
				System.out.println(errorfileName + FILE_NOT_EXIST);
				return false;
			}
			FileReader fr = new FileReader(file);
			br = new BufferedReader(fr);

			String line;
			// 一行ずつ読み込む
			while((line = br.readLine()) != null) {

				// ※ここの読み込み処理を変更してください。(処理内容1-2)
				String[] fileItems = line.split(",");

				// 定義ファイルのフォーマットチェック
				if ((fileItems.length != 2) || (!fileItems[0].matches(codeRegex))) {
					System.out.println(errorfileName + FILE_INVALID_FORMAT);
					return false;
				}

				// Mapに追加する2つの情報を putの引数として指定します。
				branchNames.put(fileItems[0], fileItems[1]);
				branchSales.put(fileItems[0], (long) 0); //固定値は明示必要
			}

		} catch(IOException e) {
			System.out.println(UNKNOWN_ERROR);
			return false;
		} finally {
			// ファイルを開いている場合
			if(br != null) {
				try {
					// ファイルを閉じる
					br.close();
				} catch(IOException e) {
					System.out.println(UNKNOWN_ERROR);
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * 支店別集計ファイル書き込み処理
	 *
	 * @param フォルダパス
	 * @param ファイル名
	 * @param 支店コードと支店名を保持するMap
	 * @param 支店コードと売上金額を保持するMap
	 * @return 書き込み可否
	 */
	private static boolean writeFile(String path, String fileName, Map<String, String> branchNames, Map<String, Long> branchSales) {
		// ※ここに書き込み処理を作成してください。(処理内容3-1)
		BufferedWriter bw = null;

		try {
			File file = new File(path,fileName);
			FileWriter fw = new FileWriter(file);
			bw = new BufferedWriter(fw);

			for (String key : branchNames.keySet()) {
				bw.write(key + "," + branchNames.get(key) + "," + branchSales.get(key));
				bw.newLine();
			}

		} catch(IOException e) {
			System.out.println(UNKNOWN_ERROR);
			return false;
		} finally {
			// ファイルを開いている場合
			if(bw != null) {
				try {
					// ファイルを閉じる
					bw.close();
				} catch(IOException e) {
					System.out.println(UNKNOWN_ERROR);
					return false;
				}
			}
		}

		return true;
	}

}
