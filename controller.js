(function () {

    "use strict";

    // ============================================================
    // NATIVE CONTROLLER BRIDGE
    // ============================================================

    window.AndroidController = {

        sendKey: function (key, down) {

            try {

                if (
                    window.AndroidKeys &&
                    typeof window.AndroidKeys.key === "function"
                ) {

                    window.AndroidKeys.key(
                        key,
                        down
                    );

                }

            } catch (e) {}

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

            } catch (e) {}

        }

    };

})();
