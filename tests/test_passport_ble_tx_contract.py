from pathlib import Path
import re


SOURCE = Path(__file__).parents[1] / "main" / "passport_ble_link.c"


def main() -> None:
    source = SOURCE.read_text(encoding="utf-8")
    notify_case = re.search(
        r"case BLE_GAP_EVENT_NOTIFY_TX:(.*?)case BLE_GAP_EVENT_ENC_CHANGE:",
        source,
        re.DOTALL,
    )
    assert notify_case is not None
    body = notify_case.group(1)
    assert "schedule_request_tx()" in body
    assert "send_next_request_frame()" not in body

    callout = re.search(
        r"static void request_tx_callout_cb\(.*?\n\}", source, re.DOTALL
    )
    assert callout is not None
    assert "send_next_request_frame()" in callout.group(0)


if __name__ == "__main__":
    main()
