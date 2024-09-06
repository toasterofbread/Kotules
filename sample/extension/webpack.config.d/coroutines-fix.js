// https://github.com/Kotlin/kotlinx.coroutines/issues/3874#issuecomment-2130428084
config.module = {
    rules: [
        {
            test: /\.js$/,
            loader: 'string-replace-loader',
            options: {
                search: 'coroutineDispatcher',
                replace: 'devToastbitsKotulesSampleExtensionCoroutineDispatcher',
                flags: 'g'
            }
        }
    ]
}
