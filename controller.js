(function () {

    "use strict";

    /*
     * Android controller bridge.
     *
     * The actual editable buttons are implemented natively
     * in MainActivity.kt so that they remain above Helmet Heroes
     * when WebView enters fullscreen.
     */

    window.AndroidController = {

        sendKey: function (name, down) {

            try {

                if (
                    window.AndroidKeys &&
                    typeof window.AndroidKeys.key === "function"
                ) {

                    window.AndroidKeys.key(
                        name,
                        down
                    );

                }

            } catch (e) {
            }
        },

        fullscreen: function () {

            try {

                if (
                    window.AndroidKeys &&
                    typeof window.AndroidKeys.key === "function"
                ) {

                    window.AndroidKeys.key(
                        "__REQUEST_GAME_FULLSCREEN__",
                        true
                    );

                }

            } catch (e) {
            }
        }

    };

})();
