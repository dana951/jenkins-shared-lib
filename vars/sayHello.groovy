def call(String name = "world") {
    echo "====================================="
    echo "Hello from Shared Library 👋"
    echo "Hello, ${name}"
    echo "Running inside Jenkins Shared Library step"
    echo "====================================="
}