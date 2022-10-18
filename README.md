# AzureのCognitiveAPIをつかって文字認識スマホアプリを作ってみた

大学の課題をやっていると紙媒体の参考文献の内容を段落ごと引用したくなる場面が出てきます。しかし段落が長ければ長いほど手動で入力していくのが非常に面倒になってきます。
そこで参考文献の目的の箇所の写真から文字データを抽出してコピペ出来るようにすればかなりの効率化につながるのではないかと思い、microsoftのAzureが提供している光学文字認識APIをつかってスマホアプリを作りました。
本当は機械学習モデルをアプリに搭載してサードパーティとの通信なしで文字認識を行えるようにしたかったのですが、メモリとCPUの性能の関係で断念し、APIという手法を採用することにしました。

以下に作成したアプリのデモgifを掲載します。(動画をgifに変換したのでカクついて見えますが実際はスムーズです)

![ImageToText-デモgif](https://user-images.githubusercontent.com/102433704/196470580-ca72633b-0ac5-447d-941c-f9a51e9d9835.gif)




