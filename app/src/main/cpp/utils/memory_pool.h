#ifndef VIDEO_EDITOR_MEMORY_POOL_H
#define VIDEO_EDITOR_MEMORY_POOL_H

#include <vector>
#include <mutex>
#include <cstdint>

namespace videoeditor {

class MemoryPool {
public:
    explicit MemoryPool(size_t blockSize, size_t numBlocks);
    ~MemoryPool();

    void* allocate();
    void deallocate(void* ptr);
    void reset();

    size_t getBlockSize() const { return m_blockSize; }
    size_t getTotalBlocks() const { return m_numBlocks; }
    size_t getFreeBlocks() const { return m_freeBlocks.size(); }

private:
    size_t m_blockSize;
    size_t m_numBlocks;
    std::vector<uint8_t> m_pool;
    std::vector<void*> m_freeBlocks;
    std::mutex m_mutex;
};

}  // namespace videoeditor

#endif  // VIDEO_EDITOR_MEMORY_POOL_H
