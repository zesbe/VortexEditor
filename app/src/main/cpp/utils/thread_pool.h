#ifndef VIDEO_EDITOR_THREAD_POOL_H
#define VIDEO_EDITOR_THREAD_POOL_H

#include <vector>
#include <queue>
#include <thread>
#include <mutex>
#include <condition_variable>
#include <functional>
#include <future>

namespace videoeditor {

class ThreadPool {
public:
    explicit ThreadPool(size_t numThreads);
    ~ThreadPool();

    template<class F, class... Args>
    auto enqueue(F&& f, Args&&... args) -> std::future<typename std::result_of<F(Args...)>::type>;

    void waitAll();
    size_t size() const { return m_workers.size(); }

private:
    std::vector<std::thread> m_workers;
    std::queue<std::function<void()>> m_tasks;
    std::mutex m_mutex;
    std::condition_variable m_condition;
    bool m_stop;
};

template<class F, class... Args>
auto ThreadPool::enqueue(F&& f, Args&&... args) -> std::future<typename std::result_of<F(Args...)>::type> {
    using return_type = typename std::result_of<F(Args...)>::type;
    
    auto task = std::make_shared<std::packaged_task<return_type()>>(
        std::bind(std::forward<F>(f), std::forward<Args>(args)...)
    );
    
    std::future<return_type> result = task->get_future();
    {
        std::unique_lock<std::mutex> lock(m_mutex);
        if (m_stop) {
            throw std::runtime_error("enqueue on stopped ThreadPool");
        }
        m_tasks.emplace([task]() { (*task)(); });
    }
    m_condition.notify_one();
    return result;
}

}  // namespace videoeditor

#endif  // VIDEO_EDITOR_THREAD_POOL_H
